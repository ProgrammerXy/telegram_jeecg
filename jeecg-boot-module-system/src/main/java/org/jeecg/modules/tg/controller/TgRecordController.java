package org.jeecg.modules.tg.controller;

import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.modules.system.entity.SysRole;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysRoleService;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.tg.entity.TgDomainConfig;
import org.jeecg.modules.tg.entity.TgRecord;
import org.jeecg.modules.tg.mapper.TgDomainConfigMapper;
import org.jeecg.modules.tg.service.ITgDomainConfigService;
import org.jeecg.modules.tg.service.ITgRecordService;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;

 /**
 * @Description: 检测记录表
 * @Author: jeecg-boot
 * @Date:   2020-12-14
 * @Version: V1.0
 */
@RestController
@RequestMapping("/org.jeecg.modules/tgRecord")
@Slf4j
public class TgRecordController extends JeecgController<TgRecord, ITgRecordService> {
	@Autowired
	private ITgRecordService tgRecordService;

	 @Resource
	 private ISysUserService sysUserService;

	 @Autowired
	 private ITgDomainConfigService domainConfigService;

	 @Resource
	 private TgDomainConfigMapper tgDomainConfigMapper;

	 @Resource
	 private ISysRoleService roleService;
	
	/**
	 * 分页列表查询
	 *
	 * @param tgRecord
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@GetMapping(value = "/list")
	public Result<?> queryPageList(TgRecord tgRecord,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   @RequestParam(name="status",defaultValue="2") Integer status,
								   HttpServletRequest req) {
		QueryWrapper<TgRecord> queryWrapper = QueryGenerator.initQueryWrapper(tgRecord, req.getParameterMap());
		LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		SysUser sysUser = sysUserService.getById(loginUser.getId());
		SysRole role = roleService.getByUserId(sysUser.getId());
		if(role==null){
			List<TgDomainConfig> domainConfigList = tgDomainConfigMapper.selectList(new QueryWrapper<TgDomainConfig>().eq("user_id", sysUser.getId()));
			List<String> list = new ArrayList<>();
			for (TgDomainConfig value:domainConfigList) {
				list.add(value.getDomain());
			}
			queryWrapper.in("domain",list);
		}
		if(status==1){
			queryWrapper.ge("status_code",400);
		}else if(status==0){
			queryWrapper.lt("status_code",400);
		}
		queryWrapper.orderBy(true,false,"create_time");
		Page<TgRecord> page = new Page<TgRecord>(pageNo, pageSize);
		IPage<TgRecord> pageList = tgRecordService.page(page, queryWrapper);
		return Result.ok(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param tgRecord
	 * @return
	 */
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody TgRecord tgRecord) {
		tgRecordService.save(tgRecord);
		return Result.ok("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param tgRecord
	 * @return
	 */
	@PutMapping(value = "/edit")
	public Result<?> edit(@RequestBody TgRecord tgRecord) {
		tgRecordService.updateById(tgRecord);
		return Result.ok("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		tgRecordService.removeById(id);
		return Result.ok("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.tgRecordService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.ok("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		TgRecord tgRecord = tgRecordService.getById(id);
		if(tgRecord==null) {
			return Result.error("未找到对应数据");
		}
		return Result.ok(tgRecord);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param tgRecord
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, TgRecord tgRecord) {
        return super.exportXls(request, tgRecord, TgRecord.class, "检测记录表");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, TgRecord.class);
    }

}
