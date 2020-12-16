package org.jeecg.modules.tg.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author programmer
 */
@Data
@Slf4j
public class IpPool implements Serializable {
    private String ip;
    private String city;
    private String port;
}
