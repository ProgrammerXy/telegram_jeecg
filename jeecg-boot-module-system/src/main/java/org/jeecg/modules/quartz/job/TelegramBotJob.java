package org.jeecg.modules.quartz.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jeecg.common.util.Http;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.RestUtil;
import org.jeecg.common.util.shell.ShellUtils;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.tg.entity.*;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.alibaba.fastjson.util.IOUtils.UTF8;
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
        ArrayList<IpPool> ipPool = new ArrayList<>(Collections.emptyList());
        try {
            //获取可靠的ipPool
            ipPool = getIpPool();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!CollectionUtils.isEmpty(ipPool)) {
            for (IpPool pool : ipPool) {
                if (!CollectionUtils.isEmpty(userList)) {
                    for (SysUser user : userList) {
                        List<TgDomainConfig> list = tgDomainConfigService.list(new LambdaQueryWrapper<TgDomainConfig>().eq(TgDomainConfig::getUserId, user.getId()));
                        if (!CollectionUtils.isEmpty(list)) {
                            for (TgDomainConfig tgDomainConfig : list) {
                                int count = 0;
                                int code = 0;
                                for (int i = 0; i < 3; i++) {
                                    String result = ShellUtils.execCmd("curl --socks5 " + pool.getIp() + ":" + pool.getPort() + "-I -m 5 -s -w \"%{http_code}\\n\"" + " -o " + " /dev/null " + tgDomainConfig.getDomain() + " --speed-time 10 --speed-limit 1");
                                    code = Integer.parseInt(result.replace("\"", ""));
                                    if (code == 000 || code > 400) {
                                        count++;
                                    }
                                }
                                if (count > 1) {
                                    code = 000;
                                }
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                ResultVo resultVo = new ResultVo();
                                TgRecord tgRecord = new TgRecord();
                                tgRecord.setCity(pool.getCity());
                                tgRecord.setIp(pool.getIp());
                                tgRecord.setDomain(tgDomainConfig.getDomain());
                                tgRecord.setStatusCode(code);
                                tgRecord.setCreateTime(new Date());
                                BeanUtils.copyProperties(tgRecord, resultVo);
                                resultVo.setUserId(tgDomainConfig.getUserId());
                                resultVo.setPort(pool.getPort());
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

    private ArrayList<IpPool> getIpPool() throws Exception {
        ArrayList<IpPool> ipPools = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : CITY_MAP.entrySet()) {
            String s = "";
            s = Http.doGet(GET_IP + entry.getValue());
            Thread.sleep(5000);
            JSONObject result = (JSONObject) JSONObject.parse(s);
            Boolean flag = result.getBoolean("success");
            if (flag) {
                JSONArray data = result.getJSONArray("data");
                JSONObject jsonObject = data.getJSONObject(0);
                String ip = jsonObject.getString("ip");
                String port = jsonObject.getString("port");
                String city = jsonObject.getString("city");
                int count = 0;
                for ( int i = 0 ;i<3;i++){
                    String checkResult = ShellUtils.execCmd("curl --socks5 " + ip + ":" + port + "-I -m 5 -s -w \"%{http_code}\\n\"" + " -o " + " /dev/null " + "www.baidu.com --speed-time 10 --speed-limit 1");
                    int code = Integer.parseInt(checkResult.replace("\"", ""));
                    if (000 == code || 400 < code){
                        count++;
                    }
                }
                if (1 > count) {
                    IpPool ipPool = new IpPool();
                    ipPool.setIp(ip);
                    ipPool.setCity(city);
                    ipPool.setPort(port);
                    ipPools.add(ipPool);
                }
            }
        }
        return ipPools;
    }

    private void getTgBotConfigByCache() {
        String data = "";
        try {
            data = Http.doGet(TG_BOT_GET_UPDATES_BY_PROXY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!StringUtils.isEmpty(data)) {
            redisUtil.del("TG_DATA");
            JSONObject parse = (JSONObject) JSONObject.parse(data);
            redisUtil.set("TG_DATA", parse);
        }
    }

    /**
     * 本次总结果集分解
     *
     * @param resultLists
     * @description 总结果集去重
     */
    private void parsingData(List<ResultVo> resultLists) {
        if (CollectionUtils.isEmpty(resultLists)) {
            return;
        }
        Set<ResultVo> setList = new LinkedHashSet<>();
        for (ResultVo resultVo : resultLists) {
            if (resultVo.getStatusCode() > 400 ) {
                setList.add(resultVo);
            }
        }
        log.info("setList is -> {}", setList);
        try {
            sendMassageByTelegramBot(setList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 结果集发送TgBot
     *
     * @param setList
     */
    private void sendMassageByTelegramBot(Set<ResultVo> setList) {
        Set<String> ids = new LinkedHashSet<>();
        for (ResultVo resultVo : setList) {
            ids.add(resultVo.getUserId());
            ArrayList<String> strInter = new ArrayList<>(ids);
            Collection<TgSendList> tgSendLists = tgSendListService.searchByUserList(strInter);
            for (TgSendList tgSendList : tgSendLists) {
                String byChatId = tgSendList.getByChatId();
                Integer type = tgSendList.getType();
                String chatId = tgSendList.getChatId();
                if (StringUtils.isEmpty(chatId)) {
                    JSONObject data = (JSONObject) redisUtil.get("TG_DATA");
                    JSONArray result = data.getJSONArray("result");
                    for (int i = 0; i < result.size(); i++) {
                        JSONObject obj = result.getJSONObject(i);
                        JSONObject message = obj.getJSONObject("message");
                        if (type == 1) {
                            JSONObject from = message.getJSONObject("from");
                            String username = from.getString("username");
                            if (!StringUtils.isEmpty(username)) {
                                if (username.equals(byChatId)) {
                                    chatId = from.getString("id");
                                    tgSendList.setChatId(chatId);
                                    tgSendListService.saveOrUpdate(tgSendList);
                                    break;
                                }
                            }
                        } else {
                            JSONObject chat = message.getJSONObject("chat");
                            String title = chat.getString("title");
                            if (!StringUtils.isEmpty(title)) {
                                if (title.equals(byChatId)) {
                                    chatId = chat.getString("id");
                                    tgSendList.setChatId(chatId);
                                    tgSendListService.saveOrUpdate(tgSendList);
                                    break;
                                }
                            }
                        }
                    }
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String format = simpleDateFormat.format(resultVo.getCreateTime());
                String variable = null;
                String encode = null;
                String string = "chat_id=" + chatId + "&text=";
                try {
                    encode = URLEncoder.encode("\n域名:         " + resultVo.getDomain() + "\n城市:         " + resultVo.getCity() + "\n" + "IP:             " + resultVo.getIp() + "\n" + "PORT:        " + resultVo.getPort() + "\n" + "状态:         异常\uD83D\uDE14" + "\n" + "时间:         " + format + "\n" + "=================================", "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                variable = string + encode;
                try {
                    String url = SEND_MASSAGES + variable;
                    JSONObject data = new JSONObject();
                    data.put("url", url);
                    RestUtil.post(SEND_MASSAGES_BY_PROXY, data);
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
