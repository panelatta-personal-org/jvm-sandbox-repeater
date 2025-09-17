package com.alibaba.repeater.console.common.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * {@link ModuleStatusDetail}
 * <p>
 * 模块状态详细信息
 *
 * @author zhaoyb1990
 */
@Getter
@Setter
public class ModuleStatusDetail implements java.io.Serializable {

    /**
     * 是否在线
     */
    private boolean online;

    /**
     * 响应时间(ms)
     */
    private long responseTime;

    /**
     * 模块是否激活
     */
    private boolean moduleActive;

    /**
     * 最后心跳时间
     */
    private Date lastHeartbeat;

    /**
     * 最后reload时间
     */
    private Date lastReloadTime;

    /**
     * 模块详细信息
     */
    private String moduleDetail;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 连续失败次数
     */
    private int failureCount;

    /**
     * 网络延迟等级
     * fast: <1000ms, normal: 1000-3000ms, slow: >3000ms
     */
    public String getLatencyLevel() {
        if (!online) return "offline";
        if (responseTime < 1000) return "fast";
        if (responseTime < 3000) return "normal";
        return "slow";
    }

    /**
     * 状态描述
     */
    public String getStatusDescription() {
        if (!online) return "离线";
        if (!moduleActive) return "异常";
        return "正常";
    }
}
