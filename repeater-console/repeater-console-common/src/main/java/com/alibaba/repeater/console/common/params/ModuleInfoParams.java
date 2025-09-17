package com.alibaba.repeater.console.common.params;

import lombok.Getter;
import lombok.Setter;

/**
 * {@link ModuleInfoParams}
 * <p>
 *
 * @author zhaoyb1990
 */
@Getter
@Setter
public class ModuleInfoParams extends BaseParams {

    private String appName;
    
    private String ip;
    
    private String port;
    
    private String environment;
}
