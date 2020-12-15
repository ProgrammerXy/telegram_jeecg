package org.jeecg.modules.tg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.tg.entity.TgSendList;
import org.jeecg.modules.tg.mapper.TgSendListMapper;
import org.jeecg.modules.tg.service.ITgSendListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 电报机器人发送列表
 * @Author: jeecg-boot
 * @Date: 2020-12-14
 * @Version: V1.0
 */
@Service
public class TgSendListServiceImpl extends ServiceImpl<TgSendListMapper, TgSendList> implements ITgSendListService {

    @Autowired
    private TgSendListMapper sendListMapper;

    @Override
    public Collection<TgSendList> searchByUserList(ArrayList<String> ids) {
        List<TgSendList> objects = Arrays.asList();
        for (String id : ids) {
            List<TgSendList> tgSendLists = sendListMapper.selectList(new LambdaQueryWrapper<TgSendList>().eq(TgSendList::getUserId, id));
            for (TgSendList tgSendList : tgSendLists ){
                objects.add(tgSendList);
            }
        }
        return objects;
    }
}
