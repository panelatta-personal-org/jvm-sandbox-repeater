package com.alibaba.repeater.console.service;

import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.repeater.console.common.domain.ModuleConfigBO;
import com.alibaba.repeater.console.common.domain.PageResult;
import com.alibaba.repeater.console.common.params.ModuleConfigParams;

/**
 * {@link ModuleConfigService}
 * <p>
 *
 * @author zhaoyb1990
 */
public interface ModuleConfigService {

    PageResult<ModuleConfigBO> list(ModuleConfigParams params);

    RepeaterResult<ModuleConfigBO> query(ModuleConfigParams params);

    RepeaterResult<ModuleConfigBO> saveOrUpdate(ModuleConfigParams params);

    RepeaterResult<ModuleConfigBO> push(ModuleConfigParams params);

    /**
     * 检查指定应用和环境的模块匹配情况
     */
    RepeaterResult<Object> checkModuleMatches(String appName, String environment);

    /**
     * 检查所有Config的Environment一致性
     */
    RepeaterResult<Object> checkEnvironments();

    /**
     * 自动修复Environment不匹配问题
     */
    RepeaterResult<String> autoFixEnvironments();

    /**
     * 调试查询所有Config数据
     */
    RepeaterResult<Object> debugQueryAllConfigs();

    /**
     * 调试匹配分析
     */
    RepeaterResult<Object> debugMatchingAnalysis(String appName, String environment);
}
