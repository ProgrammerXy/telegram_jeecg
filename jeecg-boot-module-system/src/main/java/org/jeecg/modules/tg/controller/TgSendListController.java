package org.jeecg.modules.tg.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.modules.tg.entity.TgSendList;
import org.jeecg.modules.tg.service.ITgSendListService;
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
 * @Description: 电报机器人发送列表
 * @Author: jeecg-boot
 * @Date:   2020-12-14
 * @Version: V1.0
 */
@RestController
@RequestMapping("/org.jeecg.modules/tgSendList")
@Slf4j
public class TgSendListController extends JeecgController<TgSendList, ITgSendListService> {
	@Autowired
	private ITgSendListService tgSendListService;
	
	/**
	 * 分页列表查询
	 *
	 * @param tgSendList
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@GetMapping(value = "/list")
	public Result<?> queryPageList(TgSendList tgSendList,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<TgSendList> queryWrapper = QueryGenerator.initQueryWrapper(tgSendList, req.getParameterMap());
		Page<TgSendList> page = new Page<TgSendList>(pageNo, pageSize);
		IPage<TgSendList> pageList = tgSendListService.page(page, queryWrapper);
		return Result.ok(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param tgSendList
	 * @return
	 */
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody TgSendList tgSendList) {
		tgSendListService.save(tgSendList);
		return Result.ok("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param tgSendList
	 * @return
	 */
	@PutMapping(value = "/edit")
	public Result<?> edit(@RequestBody TgSendList tgSendList) {
		tgSendListService.updateById(tgSendList);
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
		tgSendListService.removeById(id);
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
		this.tgSendListService.removeByIds(Arrays.asList(ids.split(",")));
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
		TgSendList tgSendList = tgSendListService.getById(id);
		if(tgSendList==null) {
			return Result.error("未找到对应数据");
		}
		return Result.ok(tgSendList);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param tgSendList
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, TgSendList tgSendList) {
        return super.exportXls(request, tgSendList, TgSendList.class, "电报机器人发送列表");
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
        return super.importExcel(request, response, TgSendList.class);
    }

}
