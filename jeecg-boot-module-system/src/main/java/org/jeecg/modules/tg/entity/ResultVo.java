package org.jeecg.modules.tg.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Date;

/**
 * @author programmer
 */
@Data
@Slf4j
public class ResultVo implements Serializable {
    private Integer statusCode;
    private String city;
    private String domain;
    private String userId;
    private Date createTime;
}
