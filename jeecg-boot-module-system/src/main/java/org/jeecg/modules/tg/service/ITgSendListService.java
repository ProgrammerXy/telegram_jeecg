package org.jeecg.modules.tg.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.tg.entity.TgSendList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 电报机器人发送列表
 * @Author: jeecg-boot
 * @Date:   2020-12-14
 * @Version: V1.0
 */
public interface ITgSendListService extends IService<TgSendList> {
    /**
     *
     * @param collection
     * @return
     */
    Collection<TgSendList> searchByUserList(ArrayList<String> collection);

}
