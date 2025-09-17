package com.alibaba.repeater.console.service.impl;

import com.alibaba.jvm.sandbox.repeater.plugin.Constants;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.HttpUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.repeater.console.common.domain.ModuleConfigBO;
import com.alibaba.repeater.console.common.domain.ModuleInfoBO;
import com.alibaba.repeater.console.common.domain.PageResult;
import com.alibaba.repeater.console.common.params.ModuleConfigParams;
import com.alibaba.repeater.console.common.params.ModuleInfoParams;
import com.alibaba.repeater.console.dal.dao.ModuleConfigDao;
import com.alibaba.repeater.console.dal.model.ModuleConfig;
import com.alibaba.repeater.console.service.ModuleConfigService;
import com.alibaba.repeater.console.service.ModuleInfoService;
import com.alibaba.repeater.console.service.convert.ModuleConfigConverter;
import com.alibaba.repeater.console.service.util.JacksonUtil;
import com.alibaba.repeater.console.service.util.ResultHelper;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link }
 * <p>
 *
 * @author zhaoyb1990
 */
@Service("moduleConfigService")
public class ModuleConfigServiceImpl implements ModuleConfigService {

    @Resource
    private ModuleConfigDao moduleConfigDao;
    @Resource
    private ModuleConfigConverter moduleConfigConverter;
    @Resource
    private ModuleInfoService moduleInfoService;
    @Value("${repeat.config.url}")
    private String configURL;

    @Override
    public PageResult<ModuleConfigBO> list(ModuleConfigParams params) {
        PageResult<ModuleConfigBO> result = new PageResult<>();
        Page<ModuleConfig> page = moduleConfigDao.selectByParams(params);
        if (page.hasContent()) {
            result.setSuccess(true);
            result.setPageIndex(params.getPage());
            result.setCount(page.getTotalElements());
            result.setTotalPage(page.getTotalPages());
            result.setPageSize(params.getSize());
            result.setData(page.getContent().stream().map(moduleConfigConverter::convert).collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public RepeaterResult<ModuleConfigBO> query(ModuleConfigParams params) {
        ModuleConfig moduleConfig = moduleConfigDao.query(params);
        if (moduleConfig == null) {
            return ResultHelper.fail("data not exist");
        }
        return ResultHelper.success(moduleConfigConverter.convert(moduleConfig));
    }

    @Override
    public RepeaterResult<ModuleConfigBO> saveOrUpdate(ModuleConfigParams params) {
        ModuleConfig moduleConfig = moduleConfigDao.query(params);
        if (moduleConfig != null) {
            moduleConfig.setConfig(params.getConfig());
            moduleConfig.setGmtModified(new Date());
        } else {
            moduleConfig = new ModuleConfig();
            moduleConfig.setAppName(params.getAppName());
            moduleConfig.setEnvironment(params.getEnvironment());
            moduleConfig.setConfig(params.getConfig());
            moduleConfig.setGmtCreate(new Date());
            moduleConfig.setGmtModified(new Date());
        }
        ModuleConfig callback = moduleConfigDao.saveOrUpdate(moduleConfig);
        return ResultHelper.success(moduleConfigConverter.convert(callback));
    }

    @Override
    public RepeaterResult<ModuleConfigBO> push(ModuleConfigParams params) {
        ModuleConfig moduleConfig = moduleConfigDao.query(params);
        if (moduleConfig == null) {
            return ResultHelper.fail("config not exist");
        }
        ModuleInfoParams moduleInfoParams = new ModuleInfoParams();
        moduleInfoParams.setAppName(params.getAppName());
        moduleInfoParams.setEnvironment(params.getEnvironment());
        // a temporary size set
        moduleInfoParams.setSize(1000);
        PageResult<ModuleInfoBO> result = moduleInfoService.query(moduleInfoParams);
        if (result == null || !result.isSuccess()) {
            return ResultHelper.fail("no alive module, don't need to push config.");
        }
        String data;
        try {
            RepeaterConfig config = JacksonUtil.deserialize(moduleConfig.getConfig(),RepeaterConfig.class);
            data = SerializerWrapper.hessianSerialize(config);
        } catch (SerializeException e) {
            return ResultHelper.fail("serialize config occurred error, message = " + e.getMessage());
        }
        final Map<String,String> paramMap = new HashMap<>(2);
        try {
            paramMap.put(Constants.DATA_TRANSPORT_IDENTIFY,  URLEncoder.encode(data, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return ResultHelper.fail("encode data occurred error, message = " + e.getMessage());
        }
        final Map<String,HttpUtil.Resp> respMap = Maps.newHashMap();
        result.getData().forEach(module -> {
            HttpUtil.Resp resp = HttpUtil.doGet(String.format(configURL, module.getIp(), module.getPort()), paramMap);
            respMap.put(module.getIp(), resp);
        });
        String ips = respMap.entrySet().stream().filter(entry -> !entry.getValue().isSuccess()).map(Map.Entry::getKey).collect(Collectors.joining(","));
        if (StringUtils.isNotEmpty(ips)) {
            return ResultHelper.success(ips + " push failed.");
        }
        return ResultHelper.success();
    }

    @Override
    public RepeaterResult<Object> checkModuleMatches(String appName, String environment) {
        try {
            // 查询匹配的模块
            ModuleInfoParams moduleInfoParams = new ModuleInfoParams();
            moduleInfoParams.setAppName(appName);
            moduleInfoParams.setEnvironment(environment);
            moduleInfoParams.setSize(1000);
            PageResult<ModuleInfoBO> result = moduleInfoService.query(moduleInfoParams);
            
            // 查询该应用所有可用的环境
            ModuleInfoParams allModulesParams = new ModuleInfoParams();
            allModulesParams.setAppName(appName);
            allModulesParams.setSize(1000);
            PageResult<ModuleInfoBO> allModules = moduleInfoService.query(allModulesParams);
            
            // 收集所有可用的环境
            Set<String> availableEnvironments = new HashSet<>();
            if (allModules != null && allModules.isSuccess() && allModules.getData() != null) {
                for (ModuleInfoBO module : allModules.getData()) {
                    if (module.getEnvironment() != null) {
                        availableEnvironments.add(module.getEnvironment());
                    }
                }
            }
            
            // 构造返回结果
            Map<String, Object> responseData = new HashMap<>();
            boolean hasMatches = result != null && result.isSuccess() && result.getData() != null && !result.getData().isEmpty();
            
            responseData.put("hasMatches", hasMatches);
            responseData.put("matchCount", hasMatches && result != null && result.getData() != null ? result.getData().size() : 0);
            responseData.put("availableEnvironments", new ArrayList<>(availableEnvironments));
            
            if (!hasMatches) {
                // 判断失败原因
                if (availableEnvironments.isEmpty()) {
                    responseData.put("reason", "no_modules");
                    responseData.put("suggestions", Arrays.asList(
                        "该应用没有注册任何模块",
                        "请先在模块管理页面注册模块",
                        "确认应用名称是否正确"
                    ));
                } else {
                    responseData.put("reason", "environment_mismatch");
                    responseData.put("suggestions", Arrays.asList(
                        "Environment不匹配，请修改Config的environment为: " + String.join(", ", availableEnvironments),
                        "或者注册新的模块到environment: " + environment
                    ));
                }
            } else {
                responseData.put("reason", "");
                responseData.put("suggestions", Arrays.asList("匹配正常，可以正常推送"));
            }
            
            return ResultHelper.success(responseData);
            
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("hasMatches", false);
            errorData.put("reason", "api_error");
            errorData.put("suggestions", Arrays.asList("API调用异常: " + e.getMessage()));
            errorData.put("matchCount", 0);
            errorData.put("availableEnvironments", Arrays.asList());
            return ResultHelper.success(errorData);
        }
    }

    @Override
    public RepeaterResult<Object> checkEnvironments() {
        try {
            // 获取所有Config
            List<ModuleConfig> allConfigs = moduleConfigDao.selectAll();
            
            // 获取所有Module
            ModuleInfoParams moduleParams = new ModuleInfoParams();
            moduleParams.setSize(1000);
            PageResult<ModuleInfoBO> allModulesResult = moduleInfoService.query(moduleParams);
            
            List<ModuleInfoBO> allModules = allModulesResult != null && allModulesResult.isSuccess() 
                ? allModulesResult.getData() : new ArrayList<>();
            
            // 构建模块环境映射 (appName -> Set<environment>)
            Map<String, Set<String>> moduleEnvironments = new HashMap<>();
            for (ModuleInfoBO module : allModules) {
                moduleEnvironments.computeIfAbsent(module.getAppName(), k -> new HashSet<>())
                                 .add(module.getEnvironment());
            }
            
            // 检查每个Config的匹配情况
            List<Map<String, Object>> details = new ArrayList<>();
            int issueCount = 0;
            
            for (ModuleConfig config : allConfigs) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("configAppName", config.getAppName());
                detail.put("configEnvironment", config.getEnvironment());
                
                Set<String> availableEnvs = moduleEnvironments.getOrDefault(config.getAppName(), new HashSet<>());
                detail.put("availableEnvironments", new ArrayList<>(availableEnvs));
                
                boolean matched = availableEnvs.contains(config.getEnvironment());
                detail.put("matched", matched);
                
                if (!matched) {
                    issueCount++;
                    if (availableEnvs.isEmpty()) {
                        detail.put("suggestion", "该应用没有注册任何模块");
                    } else {
                        detail.put("suggestion", "建议修改为: " + String.join(", ", availableEnvs));
                    }
                } else {
                    detail.put("suggestion", "配置正常");
                }
                
                details.add(detail);
            }
            
            // 构造返回结果
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("hasIssues", issueCount > 0);
            responseData.put("issueCount", issueCount);
            responseData.put("totalConfigs", allConfigs.size());
            responseData.put("details", details);
            
            return ResultHelper.success(responseData);
            
        } catch (Exception e) {
            return ResultHelper.fail("检查Environment失败: " + e.getMessage());
        }
    }

    @Override
    public RepeaterResult<String> autoFixEnvironments() {
        try {
            // 获取所有有问题的Config
            RepeaterResult<Object> checkResult = checkEnvironments();
            if (!checkResult.isSuccess()) {
                return ResultHelper.fail("无法获取Environment检查结果");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> checkData = (Map<String, Object>) checkResult.getData();
            Boolean hasIssues = (Boolean) checkData.get("hasIssues");
            
            if (!hasIssues) {
                return ResultHelper.success("没有发现需要修复的问题");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> details = (List<Map<String, Object>>) checkData.get("details");
            
            int fixedCount = 0;
            StringBuilder fixLog = new StringBuilder();
            
            for (Map<String, Object> detail : details) {
                Boolean matched = (Boolean) detail.get("matched");
                if (!matched) {
                    String configAppName = (String) detail.get("configAppName");
                    String configEnvironment = (String) detail.get("configEnvironment");
                    @SuppressWarnings("unchecked")
                    List<String> availableEnvironments = (List<String>) detail.get("availableEnvironments");
                    
                    if (!availableEnvironments.isEmpty()) {
                        // 选择第一个可用的环境进行修复
                        String targetEnvironment = availableEnvironments.get(0);
                        
                        // 更新Config的environment
                        ModuleConfigParams updateParams = new ModuleConfigParams();
                        updateParams.setAppName(configAppName);
                        updateParams.setEnvironment(configEnvironment);
                        
                        ModuleConfig existingConfig = moduleConfigDao.query(updateParams);
                        if (existingConfig != null) {
                            existingConfig.setEnvironment(targetEnvironment);
                            moduleConfigDao.save(existingConfig);
                            
                            fixedCount++;
                            fixLog.append(String.format("修复 %s: %s -> %s; ", 
                                configAppName, configEnvironment, targetEnvironment));
                        }
                    }
                }
            }
            
            if (fixedCount > 0) {
                return ResultHelper.success(String.format("成功修复 %d 个配置。%s", fixedCount, fixLog.toString()));
            } else {
                return ResultHelper.success("没有可以自动修复的配置");
            }
            
        } catch (Exception e) {
            return ResultHelper.fail("自动修复失败: " + e.getMessage());
        }
    }

    @Override
    public RepeaterResult<Object> debugQueryAllConfigs() {
        try {
            List<ModuleConfig> allConfigs = moduleConfigDao.selectAll();
            
            List<Map<String, Object>> configList = new ArrayList<>();
            for (ModuleConfig config : allConfigs) {
                Map<String, Object> configData = new HashMap<>();
                configData.put("appName", config.getAppName());
                configData.put("environment", config.getEnvironment());
                configData.put("gmtCreate", config.getGmtCreate());
                configData.put("gmtModified", config.getGmtModified());
                configData.put("config", config.getConfig());
                configData.put("configContent", config.getConfig() != null ? 
                    config.getConfig().substring(0, Math.min(100, config.getConfig().length())) + "..." : "");
                configList.add(configData);
            }
            
            return ResultHelper.success(configList);
            
        } catch (Exception e) {
            return ResultHelper.fail("查询Config数据失败: " + e.getMessage());
        }
    }

    @Override
    public RepeaterResult<Object> debugMatchingAnalysis(String appName, String environment) {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // 1. 查询匹配的模块
            ModuleInfoParams moduleParams = new ModuleInfoParams();
            moduleParams.setAppName(appName);
            moduleParams.setEnvironment(environment);
            moduleParams.setSize(1000);
            PageResult<ModuleInfoBO> moduleResult = moduleInfoService.query(moduleParams);
            
            List<ModuleInfoBO> matchedModules = new ArrayList<>();
            if (moduleResult != null && moduleResult.isSuccess() && moduleResult.getData() != null) {
                matchedModules = moduleResult.getData();
            }
            
            // 2. 查询匹配的配置
            ModuleConfigParams configParams = new ModuleConfigParams();
            configParams.setAppName(appName);
            configParams.setEnvironment(environment);
            List<ModuleConfig> matchedConfigs = new ArrayList<>();
            try {
                ModuleConfig config = moduleConfigDao.query(configParams);
                if (config != null) {
                    matchedConfigs.add(config);
                }
            } catch (Exception e) {
                // 配置不存在，忽略
            }
            
            // 3. 分析问题和建议
            List<String> issues = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();
            
            if (matchedModules.isEmpty()) {
                issues.add("没有找到匹配的模块 (应用名: " + appName + ", 环境: " + environment + ")");
                
                // 查询该应用的所有模块
                ModuleInfoParams allModulesParams = new ModuleInfoParams();
                allModulesParams.setAppName(appName);
                allModulesParams.setSize(1000);
                PageResult<ModuleInfoBO> allModulesResult = moduleInfoService.query(allModulesParams);
                
                if (allModulesResult != null && allModulesResult.isSuccess() && 
                    allModulesResult.getData() != null && !allModulesResult.getData().isEmpty()) {
                    
                    Set<String> availableEnvs = new HashSet<>();
                    for (ModuleInfoBO module : allModulesResult.getData()) {
                        if (module.getEnvironment() != null) {
                            availableEnvs.add(module.getEnvironment());
                        }
                    }
                    
                    if (!availableEnvs.isEmpty()) {
                        suggestions.add("该应用存在以下环境的模块: " + String.join(", ", availableEnvs));
                        suggestions.add("请检查环境名称是否正确，或注册新的模块到环境: " + environment);
                    } else {
                        suggestions.add("该应用没有注册任何模块，请先在模块管理页面注册模块");
                    }
                } else {
                    suggestions.add("该应用没有注册任何模块，请先在模块管理页面注册模块");
                }
            } else {
                suggestions.add("找到 " + matchedModules.size() + " 个匹配的模块，模块状态正常");
            }
            
            if (matchedConfigs.isEmpty()) {
                issues.add("没有找到匹配的配置 (应用名: " + appName + ", 环境: " + environment + ")");
                suggestions.add("请在配置管理页面创建对应的配置");
            } else {
                suggestions.add("找到 " + matchedConfigs.size() + " 个匹配的配置");
            }
            
            // 4. 构建返回结果
            analysis.put("moduleCount", matchedModules.size());
            analysis.put("configCount", matchedConfigs.size());
            analysis.put("modules", matchedModules);
            analysis.put("configs", matchedConfigs);
            analysis.put("issues", issues);
            analysis.put("suggestions", suggestions);
            
            return ResultHelper.success(analysis);
            
        } catch (Exception e) {
            return ResultHelper.fail("匹配分析失败: " + e.getMessage());
        }
    }

    @Override
    public RepeaterResult<Object> getK8sInstanceInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            
            // 获取系统环境变量（K8S常用的环境变量）
            info.put("podName", System.getenv("HOSTNAME"));
            info.put("podIp", System.getenv("POD_IP"));
            info.put("podNamespace", System.getenv("POD_NAMESPACE"));
            info.put("nodeName", System.getenv("NODE_NAME"));
            info.put("nodeIp", System.getenv("NODE_IP"));
            
            // 获取JVM信息
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("osName", System.getProperty("os.name"));
            info.put("osArch", System.getProperty("os.arch"));
            
            // 获取时间信息
            info.put("timestamp", new Date().toString());
            info.put("timezone", java.util.TimeZone.getDefault().getID());
            info.put("currentTimeMillis", System.currentTimeMillis());
            
            // 获取运行时信息
            Runtime runtime = Runtime.getRuntime();
            info.put("availableProcessors", runtime.availableProcessors());
            info.put("totalMemory", runtime.totalMemory());
            info.put("freeMemory", runtime.freeMemory());
            info.put("maxMemory", runtime.maxMemory());
            
            // 尝试获取容器ID（如果在Docker容器中）
            try {
                java.io.File cgroupFile = new java.io.File("/proc/self/cgroup");
                if (cgroupFile.exists()) {
                    java.util.Scanner scanner = new java.util.Scanner(cgroupFile);
                    if (scanner.hasNextLine()) {
                        String firstLine = scanner.nextLine();
                        if (firstLine.contains("docker")) {
                            String[] parts = firstLine.split("/");
                            if (parts.length > 0) {
                                String containerId = parts[parts.length - 1];
                                if (containerId.length() > 12) {
                                    info.put("containerId", containerId.substring(0, 12));
                                }
                            }
                        }
                    }
                    scanner.close();
                }
            } catch (Exception e) {
                // 忽略容器ID获取失败
            }
            
            return ResultHelper.success(info);
            
        } catch (Exception e) {
            return ResultHelper.fail("获取K8S实例信息失败: " + e.getMessage());
        }
    }

    @Override
    public RepeaterResult<Object> getK8sDatabaseInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            
            // 获取数据库连接信息（通过系统属性和环境变量）
            try {
                // 尝试从环境变量获取数据库信息
                String dbUrl = System.getenv("DATABASE_URL");
                if (dbUrl == null) {
                    dbUrl = System.getProperty("spring.datasource.url");
                }
                if (dbUrl == null) {
                    dbUrl = System.getProperty("db.url");
                }
                
                String dbUsername = System.getenv("DATABASE_USERNAME");
                if (dbUsername == null) {
                    dbUsername = System.getProperty("spring.datasource.username");
                }
                
                String dbDriver = System.getenv("DATABASE_DRIVER");
                if (dbDriver == null) {
                    dbDriver = System.getProperty("spring.datasource.driver-class-name");
                }
                
                info.put("databaseUrl", dbUrl != null ? dbUrl : "Unknown");
                info.put("databaseUsername", dbUsername != null ? dbUsername : "Unknown");
                info.put("databaseDriver", dbDriver != null ? dbDriver : "Unknown");
                
                // 添加一些常见的数据库配置环境变量
                info.put("dbHost", System.getenv("DB_HOST"));
                info.put("dbPort", System.getenv("DB_PORT"));
                info.put("dbName", System.getenv("DB_NAME"));
                
            } catch (Exception e) {
                info.put("dataSourceError", e.getMessage());
            }
            
            // 查询数据库统计信息
            try {
                List<ModuleConfig> allConfigs = moduleConfigDao.selectAll();
                info.put("configCount", allConfigs.size());
                
                if (!allConfigs.isEmpty()) {
                    Date latestConfigTime = null;
                    for (ModuleConfig config : allConfigs) {
                        Date modified = config.getGmtModified();
                        if (modified != null && (latestConfigTime == null || modified.after(latestConfigTime))) {
                            latestConfigTime = modified;
                        }
                    }
                    info.put("latestConfigTime", latestConfigTime);
                }
                
                // 获取模块统计
                ModuleInfoParams moduleParams = new ModuleInfoParams();
                moduleParams.setSize(1000);
                PageResult<ModuleInfoBO> moduleResult = moduleInfoService.query(moduleParams);
                if (moduleResult != null && moduleResult.isSuccess() && moduleResult.getData() != null) {
                    info.put("moduleCount", moduleResult.getData().size());
                    
                    Date latestModuleTime = null;
                    for (ModuleInfoBO module : moduleResult.getData()) {
                        Date modified = module.getGmtModified();
                        if (modified != null && (latestModuleTime == null || modified.after(latestModuleTime))) {
                            latestModuleTime = modified;
                        }
                    }
                    info.put("latestModuleTime", latestModuleTime);
                }
                
            } catch (Exception e) {
                info.put("queryError", e.getMessage());
            }
            
            // 检查数据库连接状态
            try {
                // 简单的数据库连接测试
                long startTime = System.currentTimeMillis();
                moduleConfigDao.selectAll(); // 执行一个简单查询
                long queryTime = System.currentTimeMillis() - startTime;
                info.put("connectionStatus", "OK");
                info.put("queryResponseTime", queryTime + "ms");
            } catch (Exception e) {
                info.put("connectionStatus", "ERROR: " + e.getMessage());
            }
            
            return ResultHelper.success(info);
            
        } catch (Exception e) {
            return ResultHelper.fail("获取K8S数据库信息失败: " + e.getMessage());
        }
    }
}
