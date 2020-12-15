package org.jeecg.modules.quartz.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.Http;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.shell.ShellUtils;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.tg.entity.ResultVo;
import org.jeecg.modules.tg.entity.TgDomainConfig;
import org.jeecg.modules.tg.entity.TgRecord;
import org.jeecg.modules.tg.entity.TgSendList;
import org.jeecg.modules.tg.service.ITgDomainConfigService;
import org.jeecg.modules.tg.service.ITgRecordService;
import org.jeecg.modules.tg.service.ITgSendListService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import static org.jeecg.common.constant.UrlConstant.*;

/**
 * @author programmer
 */
@Slf4j
public class TelegramBotJob implements Job {
    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ITgDomainConfigService tgDomainConfigService;

    @Autowired
    private ITgRecordService tgRecordService;

    @Autowired
    private ITgSendListService tgSendListService;

    @Autowired
    private RedisUtil redisUtil;

    private static final HashMap<String, Integer> CITY_MAP = new HashMap(8);

    static {
        CITY_MAP.put("北京市", 110105);
        CITY_MAP.put("上海市", 310112);
        CITY_MAP.put("广东省", 440500);
        CITY_MAP.put("湖南省", 430600);
        CITY_MAP.put("湖北省", 420100);
        CITY_MAP.put("山东省", 370100);
        CITY_MAP.put("黑龙江省", 230600);
        CITY_MAP.put("江苏省", 320100);
        CITY_MAP.put("浙江省", 330100);
        CITY_MAP.put("陕西省", 610100);
        CITY_MAP.put("四川省", 510501);
        CITY_MAP.put("重庆市", 500300);
        CITY_MAP.put("安徽省", 340100);
        CITY_MAP.put("福建省", 350100);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //每次获取TG机器人最新订阅
        getTgBotConfigByCache();
        //获得所有用户排除管理员
        List<SysUser> userList = sysUserService.list(new LambdaQueryWrapper<SysUser>().notLike(SysUser::getUsername, "admin")
                .notLike(SysUser::getUsername, "tgadmin")
                .notLike(SysUser::getUsername, "jeecg"));
        List<ResultVo> resultVos = new ArrayList<>(Collections.emptyList());
        for (Map.Entry<String, Integer> entry : CITY_MAP.entrySet()) {
            String s = null;
            try {
                s = Http.doGet(GET_IP + entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!StringUtils.isEmpty(s)) {
                JSONObject parse = (JSONObject) JSONObject.parse(s);
                JSONArray data = parse.getJSONArray("data");
                JSONObject jsonObject = data.getJSONObject(0);
                String ip = jsonObject.getString("ip");
                String city = jsonObject.getString("city");
                String port = jsonObject.getString("port");
                if (!CollectionUtils.isEmpty(userList)) {
                    for (SysUser user : userList) {
                        List<TgDomainConfig> list = tgDomainConfigService.list(new LambdaQueryWrapper<TgDomainConfig>().eq(TgDomainConfig::getUserId, user.getId()));
                        if (!CollectionUtils.isEmpty(list)) {
                            for (TgDomainConfig tgDomainConfig : list) {
                                String result = ShellUtils.execCmd("curl --socks5 " + ip + ":" + port + "-I -m 5 -s -w \"%{http_code}\\n\"" + " -o " + " /dev/null " + tgDomainConfig.getDomain());
                                int i = Integer.parseInt(result.replace("\"", ""));
                                ResultVo resultVo = new ResultVo();
                                TgRecord tgRecord = new TgRecord();
                                tgRecord.setCity(city);
                                tgRecord.setIp(ip);
                                tgRecord.setDomain(tgDomainConfig.getDomain());
                                tgRecord.setStatusCode(i);
                                tgRecord.setCreateTime(new Date());
                                BeanUtils.copyProperties(tgRecord, resultVo);
                                resultVo.setUserId(tgDomainConfig.getUserId());
                                resultVos.add(resultVo);
                                tgRecordService.save(tgRecord);
                            }
                        }
                    }
                }
            }
        }
        parsingData(resultVos);
    }

    private void getTgBotConfigByCache() {
        String data = "";
        try {
            data = Http.doGet(TG_BOT_GET_UPDATES);
        } catch (Exception e) {
            e.printStackTrace();
        }
        redisUtil.del("TG_DATA");
        JSONObject parse = (JSONObject) JSONObject.parse(data);
        redisUtil.set("TG_DATA", parse);
    }

    /**
     * 本次总结果集分解
     *
     * @param resultLists
     * @description 总结果集去重
     */
    private void parsingData(List<ResultVo> resultLists) {
        Set<ResultVo> setList = new LinkedHashSet<>();
        for (ResultVo resultVo : resultLists) {
            if (resultVo.getStatusCode() > 400 || resultVo.getStatusCode() == 000) {
                setList.add(resultVo);
            }
        }
        log.info("setList is -> {}",setList);
        sendMassageByTelegramBot(setList);
    }

    /**
     * 结果集发送TgBot
     *
     * @param setList
     */
    private void sendMassageByTelegramBot(Set<ResultVo> setList) {
        ArrayList<List<TgSendList>> lists = new ArrayList<>();
        setList.forEach(item ->{
            lists.add(tgSendListService.list(new LambdaQueryWrapper<TgSendList>().eq(TgSendList::getUserId, item.getUserId())));
        });
        lists.forEach(System.out::println);
//        try {
//            JSONObject tg_data = (JSONObject) redisUtil.get("TG_DATA");
//            Http.doGet(SEND_MASSAGES);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

}
