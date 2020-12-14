package org.jeecg.modules.quartz.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecg.modules.tg.entity.TgDomainConfig;
import org.jeecg.modules.tg.service.ITgDomainConfigService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author programmer
 */
@Slf4j
public class TelegramBotJob implements Job {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ITgDomainConfigService tgDomainConfigService;

    private static final HashMap<String, Integer> cityMap = new HashMap();

    static {
        cityMap.put("北京市",110105);
        cityMap.put("上海市",310112);
        cityMap.put("广东省",440500);
        cityMap.put("湖南省",430600);
        cityMap.put("湖北省",420100);
        cityMap.put("山东省",370100);
        cityMap.put("黑龙江省",230600);
        cityMap.put("江苏省",320100);
        cityMap.put("浙江省",330100);
        cityMap.put("陕西省",610100);
        cityMap.put("四川省",510501);
        cityMap.put("重庆市",500300);
        cityMap.put("安徽省",340100);
        cityMap.put("福建省",350100);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //获得所有用户排除管理员
        List<SysUser> userList = sysUserService.list(new LambdaQueryWrapper<SysUser>().notLike(SysUser::getUsername, "admin")
                .notLike(SysUser::getUsername, "tgadmin")
                .notLike(SysUser::getUsername, "jeecg"));
        List<String> ids = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userList)) {
            for (SysUser user : userList) {
                ids.add(user.getId());
            }
        }
        List<TgDomainConfig> tgDomainConfigs = (List<TgDomainConfig>) tgDomainConfigService.listByIds(ids);

    }

    private void setIpAddress(){

    }
}
