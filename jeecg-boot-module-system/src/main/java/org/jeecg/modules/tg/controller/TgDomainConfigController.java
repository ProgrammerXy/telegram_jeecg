package org.jeecg.modules.tg.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.core.assist.ISqlRunner;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.service.ISysRoleService;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.tg.entity.TgDomainConfig;
import org.jeecg.modules.tg.service.ITgDomainConfigService;
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
 * @Description: 域名配置表
 * @Author: jeecg-boot
 * @Date:   2020-12-14
 * @Version: V1.0
 */
@RestController
@RequestMapping("/org.jeecg.modules/tgDomainConfig")
@Slf4j
public class TgDomainConfigController extends JeecgController<TgDomainConfig, ITgDomainConfigService> {
	@Autowired
	private ITgDomainConfigService tgDomainConfigService;

	@Resource
	private ISysUserService sysUserService;

	@Resource
	private ISysRoleService roleService;


	 /**
	 * 分页列表查询
	 *
	 * @param tgDomainConfig
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@GetMapping(value = "/list")
	public Result<?> queryPageList(TgDomainConfig tgDomainConfig,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		SysUser sysUser = sysUserService.getById(loginUser.getId());
		SysRole role = roleService.getByUserId(sysUser.getId());
		QueryWrapper<TgDomainConfig> queryWrapper = QueryGenerator.initQueryWrapper(tgDomainConfig, req.getParameterMap());
		if(role==null){
			queryWrapper.eq("user_id",loginUser.getId());
		}
		if(sysUser.getRealname().equals("admin")){}
		Page<TgDomainConfig> page = new Page<TgDomainConfig>(pageNo, pageSize);
		IPage<TgDomainConfig> pageList = tgDomainConfigService.page(page, queryWrapper);
		return Result.ok(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param tgDomainConfig
	 * @return
	 */
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody TgDomainConfig tgDomainConfig) {
		LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		SysUser sysUser = sysUserService.getById(loginUser.getId());
		tgDomainConfig.setUserId(sysUser.getId());
		tgDomainConfigService.save(tgDomainConfig);
		return Result.ok("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param tgDomainConfig
	 * @return
	 */
	@PutMapping(value = "/edit")
	public Result<?> edit(@RequestBody TgDomainConfig tgDomainConfig) {
		tgDomainConfigService.updateById(tgDomainConfig);
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
		tgDomainConfigService.removeById(id);
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
		this.tgDomainConfigService.removeByIds(Arrays.asList(ids.split(",")));
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
		TgDomainConfig tgDomainConfig = tgDomainConfigService.getById(id);
		if(tgDomainConfig==null) {
			return Result.error("未找到对应数据");
		}
		return Result.ok(tgDomainConfig);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param tgDomainConfig
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, TgDomainConfig tgDomainConfig) {
        return super.exportXls(request, tgDomainConfig, TgDomainConfig.class, "域名配置表");
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
        return super.importExcel(request, response, TgDomainConfig.class);
    }

}
