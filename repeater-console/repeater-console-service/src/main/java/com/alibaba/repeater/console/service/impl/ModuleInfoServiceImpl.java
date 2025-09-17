package com.alibaba.repeater.console.service.impl;

import com.alibaba.jvm.sandbox.repeater.plugin.core.util.HttpUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.repeater.console.common.domain.ModuleInfoBO;
import com.alibaba.repeater.console.common.domain.ModuleStatus;
import com.alibaba.repeater.console.common.domain.ModuleStatusDetail;
import com.alibaba.repeater.console.common.domain.PageResult;
import com.alibaba.repeater.console.common.params.ModuleInfoParams;
import com.alibaba.repeater.console.dal.dao.ModuleInfoDao;
import com.alibaba.repeater.console.dal.model.ModuleInfo;
import com.alibaba.repeater.console.service.ModuleInfoService;
import com.alibaba.repeater.console.service.convert.ModuleInfoConverter;
import com.alibaba.repeater.console.service.util.ResultHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link ModuleInfoServiceImpl}
 * <p>
 *
 * @author zhaoyb1990
 */
@Service("heartbeatService")
public class ModuleInfoServiceImpl implements ModuleInfoService {

    private static String activeURI = "http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/active?ids=repeater";

    private static String frozenURI = "http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/frozen?ids=repeater";

    @Value("${repeat.reload.url}")
    private String reloadURI;


    @Resource
    private ModuleInfoDao moduleInfoDao;

    @Resource
    private ModuleInfoConverter moduleInfoConverter;

    @Resource
    private MessageSource messageSource;

    /**
     * 获取国际化消息
     */
    private String getMessage(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, locale);
    }

    @Override
    public PageResult<ModuleInfoBO> query(ModuleInfoParams params) {
        Page<ModuleInfo> page = moduleInfoDao.selectByParams(params);
        PageResult<ModuleInfoBO> result = new PageResult<>();
        if (page.hasContent()) {
            result.setSuccess(true);
            result.setPageIndex(params.getPage());
            result.setCount(page.getTotalElements());
            result.setPageSize(params.getSize());
            result.setTotalPage(page.getTotalPages());
            result.setData(page.getContent().stream().map(moduleInfoConverter::convert).collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public RepeaterResult<List<ModuleInfoBO>> query(String appName) {
        List<ModuleInfo> byAppName = moduleInfoDao.findByAppName(appName);
        if (CollectionUtils.isEmpty(byAppName)) {
            return ResultHelper.fail("data not exist");
        }
        return ResultHelper.success(
                byAppName.stream().map(moduleInfoConverter::convert).collect(Collectors.toList())
        );
    }

    @Override
    public RepeaterResult<ModuleInfoBO> query(String appName, String ip) {
        ModuleInfo moduleInfo = moduleInfoDao.findByAppNameAndIp(appName, ip);
        if (moduleInfo == null) {
            return RepeaterResult.builder().message("data not exist").build();
        }
        return ResultHelper.success(moduleInfoConverter.convert(moduleInfo));
    }

    @Override
    public RepeaterResult<ModuleInfoBO> report(ModuleInfoBO params) {
        // 查找已存在的模块
        ModuleInfo existingModule = moduleInfoDao.findByAppNameAndIp(params.getAppName(), params.getIp());
        if (existingModule != null) {
            // 只更新心跳时间，不更新创建时间
            existingModule.setGmtModified(new Date());
            if (params.getStatus() != null) {
                existingModule.setStatus(params.getStatus().name());
            }
            // 如果有版本信息，更新版本
            if (params.getVersion() != null && !params.getVersion().isEmpty()) {
                existingModule.setVersion(params.getVersion());
            }
            moduleInfoDao.saveAndFlush(existingModule);
            return ResultHelper.success(moduleInfoConverter.convert(existingModule));
        } else {
            // 新模块，设置创建时间和心跳时间
            ModuleInfo moduleInfo = moduleInfoConverter.reconvert(params);
            moduleInfo.setGmtCreate(new Date());
            moduleInfo.setGmtModified(new Date());
            moduleInfoDao.save(moduleInfo);
            return ResultHelper.success(moduleInfoConverter.convert(moduleInfo));
        }
    }

    @Override
    public RepeaterResult<ModuleInfoBO> active(ModuleInfoParams params) {
        return execute(activeURI, params, ModuleStatus.ACTIVE);
    }

    @Override
    public RepeaterResult<ModuleInfoBO> frozen(ModuleInfoParams params) {
        return execute(frozenURI, params, ModuleStatus.FROZEN);
    }

    @Override
    public RepeaterResult<String> install(ModuleInfoParams params) {
        if (StringUtils.isEmpty(params.getIp()) || StringUtils.isEmpty(params.getAppName())) {
            return ResultHelper.fail(getMessage("error.ip.appname.required"));
        }
        
        // 设置默认端口
        String port = StringUtils.isEmpty(params.getPort()) ? "12580" : params.getPort();
        
        // 设置默认环境
        String environment = StringUtils.isEmpty(params.getEnvironment()) ? "default" : params.getEnvironment();
        
        // 验证端口范围
        try {
            int portNum = Integer.parseInt(port);
            if (portNum < 1 || portNum > 65535) {
                return ResultHelper.fail(getMessage("error.port.range"));
            }
        } catch (NumberFormatException e) {
            return ResultHelper.fail(getMessage("error.port.format"));
        }
        
        try {
            // 1. 检查是否已经注册过该模块
            ModuleInfo existingModule = moduleInfoDao.findByAppNameAndIp(params.getAppName(), params.getIp());
            if (existingModule != null) {
                return ResultHelper.fail(getMessage("error.module.already.registered"));
            }
            
            // 2. 验证远程模块是否可连接
            String testUrl = String.format("http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/list", 
                                           params.getIp(), port);
            HttpUtil.Resp resp = HttpUtil.doGet(testUrl);
            
            if (!resp.isSuccess()) {
                return ResultHelper.fail(getMessage("error.connection.failed", 
                    params.getIp(), port, resp.getMessage()));
            }
            
            // 3. 进一步验证repeater模块是否存在并获取版本信息
            String repeaterCheckUrl = String.format(
                "http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/detail?id=repeater", 
                params.getIp(), port);
            HttpUtil.Resp repeaterResp = HttpUtil.doGet(repeaterCheckUrl);
            
            if (!repeaterResp.isSuccess()) {
                return ResultHelper.fail(getMessage("error.repeater.not.found", 
                    params.getIp(), port));
            }
            
            // 4. 解析版本信息
            String version = parseVersionFromSandboxResponse(repeaterResp.getBody());
            
            // 5. 注册模块信息到数据库
            ModuleInfo moduleInfo = new ModuleInfo();
            moduleInfo.setAppName(params.getAppName());
            moduleInfo.setIp(params.getIp());
            moduleInfo.setPort(port);
            moduleInfo.setEnvironment(environment);  // 设置环境
            moduleInfo.setVersion(version);          // 设置解析得到的版本
            moduleInfo.setStatus(ModuleStatus.ACTIVE.name());
            moduleInfo.setGmtCreate(new Date());
            moduleInfo.setGmtModified(new Date());
            
            moduleInfoDao.save(moduleInfo);
            
            return ResultHelper.success(getMessage("success.module.registered"), 
                getMessage("success.module.registered.detail", 
                    params.getAppName(), params.getIp(), port, environment, version));
                
        } catch (Exception e) {
            return ResultHelper.fail(getMessage("error.registration.failed", e.getMessage()));
        }
    }

    @Override
    public RepeaterResult<String> reload(ModuleInfoParams params) {
        ModuleInfo moduleInfo = moduleInfoDao.findByAppNameAndIp(params.getAppName(), params.getIp());
        if (moduleInfo == null) {
            return ResultHelper.fail(getMessage("error.module.not.found"));
        }
        
        long startTime = System.currentTimeMillis();
        String reloadUrl = String.format(reloadURI, moduleInfo.getIp(), moduleInfo.getPort());
        
        try {
            HttpUtil.Resp resp = HttpUtil.doGet(reloadUrl);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (resp.isSuccess()) {
                // 1. 更新reload时间
                moduleInfo.setGmtModified(new Date());
                moduleInfoDao.saveAndFlush(moduleInfo);
                
                // 2. 验证reload后的模块状态
                boolean moduleActive = verifyModuleStatus(moduleInfo.getIp(), moduleInfo.getPort());
                
                return ResultHelper.success(
                    getMessage("success.module.reloaded", responseTime),
                    String.format("模块reload成功，响应时间: %dms，状态: %s", 
                        responseTime, moduleActive ? "正常" : "异常")
                );
            } else {
                return ResultHelper.fail(
                    getMessage("error.reload.failed", resp.getMessage())
                );
            }
        } catch (Exception e) {
            return ResultHelper.fail(
                getMessage("error.reload.exception", e.getMessage())
            );
        }
    }

    /**
     * 验证模块状态
     */
    private boolean verifyModuleStatus(String ip, String port) {
        try {
            String statusUrl = String.format("http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/detail?id=repeater", ip, port);
            HttpUtil.Resp resp = HttpUtil.doGet(statusUrl);
            return resp.isSuccess() && resp.getBody().contains("ACTIVE");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取模块详细状态
     */
    public RepeaterResult<ModuleStatusDetail> getModuleStatus(ModuleInfoParams params) {
        ModuleInfo moduleInfo = moduleInfoDao.findByAppNameAndIp(params.getAppName(), params.getIp());
        if (moduleInfo == null) {
            return ResultHelper.fail(getMessage("error.module.not.found"));
        }
        
        ModuleStatusDetail status = new ModuleStatusDetail();
        status.setLastHeartbeat(moduleInfo.getGmtModified());
        long startTime = System.currentTimeMillis();
        
        try {
            // 检测连接状态
            String pingUrl = String.format("http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/list", 
                                         moduleInfo.getIp(), moduleInfo.getPort());
            HttpUtil.Resp resp = HttpUtil.doGet(pingUrl);
            
            status.setOnline(resp.isSuccess());
            status.setResponseTime(System.currentTimeMillis() - startTime);
            
            if (resp.isSuccess()) {
                // 获取详细状态信息
                String detailUrl = String.format("http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/detail?id=repeater", 
                                                moduleInfo.getIp(), moduleInfo.getPort());
                HttpUtil.Resp detailResp = HttpUtil.doGet(detailUrl);
                if (detailResp.isSuccess()) {
                    status.setModuleActive(detailResp.getBody().contains("ACTIVE"));
                    status.setModuleDetail(detailResp.getBody());
                } else {
                    status.setModuleActive(false);
                }
                
                // 重置失败计数
                status.setFailureCount(0);
            } else {
                status.setModuleActive(false);
                status.setError(resp.getMessage());
                status.setFailureCount(1);
            }
            
            return ResultHelper.success(status);
        } catch (Exception e) {
            status.setOnline(false);
            status.setModuleActive(false);
            status.setError(e.getMessage());
            status.setFailureCount(1);
            status.setResponseTime(System.currentTimeMillis() - startTime);
            return ResultHelper.success(status);
        }
    }

    private RepeaterResult<ModuleInfoBO> execute(String uri, ModuleInfoParams params, ModuleStatus finishStatus) {
        ModuleInfo moduleInfo = moduleInfoDao.findByAppNameAndIp(params.getAppName(), params.getIp());
        if (moduleInfo == null) {
            return ResultHelper.fail("data not exist");
        }
        HttpUtil.Resp resp = HttpUtil.doGet(String.format(uri, moduleInfo.getIp(), moduleInfo.getPort()));
        if (!resp.isSuccess()) {
            return ResultHelper.fail(resp.getMessage());
        }
        moduleInfo.setStatus(finishStatus.name());
        moduleInfo.setGmtModified(new Date());
        moduleInfoDao.saveAndFlush(moduleInfo);
        return ResultHelper.success(moduleInfoConverter.convert(moduleInfo));
    }

    @Override
    public RepeaterResult<String> remove(ModuleInfoParams params) {
        if (StringUtils.isEmpty(params.getIp()) || StringUtils.isEmpty(params.getAppName())) {
            return ResultHelper.fail(getMessage("error.ip.appname.required"));
        }
        
        ModuleInfo moduleInfo = moduleInfoDao.findByAppNameAndIp(params.getAppName(), params.getIp());
        if (moduleInfo == null) {
            return ResultHelper.fail(getMessage("error.module.not.exist"));
        }
        
        // 可选：验证端口是否匹配（如果前端传递了端口参数）
        if (!StringUtils.isEmpty(params.getPort()) && 
            !params.getPort().equals(moduleInfo.getPort())) {
            return ResultHelper.fail(getMessage("error.port.mismatch"));
        }
        
        moduleInfoDao.delete(moduleInfo);
        return ResultHelper.success(getMessage("success.module.removed", 
            moduleInfo.getIp(), moduleInfo.getPort()));
    }

    /**
     * 从sandbox响应中解析版本信息
     * @param responseContent sandbox API响应内容
     * @return 版本信息，解析失败时返回"unknown"
     */
    private String parseVersionFromSandboxResponse(String responseContent) {
        try {
            if (StringUtils.isEmpty(responseContent)) {
                return "unknown";
            }
            
            // 方法1：专门处理sandbox文本格式（优先级最高）
            // 匹配实际格式：" VERSION : 1.0.0"
            java.util.regex.Pattern sandboxTextPattern = java.util.regex.Pattern.compile(
                "VERSION\\s*:\\s*([0-9\\.\\-a-zA-Z]+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher sandboxMatcher = sandboxTextPattern.matcher(responseContent);
            if (sandboxMatcher.find()) {
                return sandboxMatcher.group(1);
            }
            
            // 方法2：处理JSON格式（向后兼容）
            // 匹配格式：{"version":"1.0.0"} 或 "version": "1.0.0"
            java.util.regex.Pattern jsonPattern = java.util.regex.Pattern.compile(
                "\"version\"\\s*:\\s*\"([^\"]+)\"", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher jsonMatcher = jsonPattern.matcher(responseContent);
            if (jsonMatcher.find()) {
                return jsonMatcher.group(1);
            }
            
            // 方法3：通用版本模式（支持多种分隔符）
            // 匹配格式：version=1.0.0, version: 1.0.0, version 1.0.0 等
            java.util.regex.Pattern generalPattern = java.util.regex.Pattern.compile(
                "version[\"\\s]*[:=\\s]+[\"\\s]*([0-9\\.\\-a-zA-Z]+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher generalMatcher = generalPattern.matcher(responseContent);
            if (generalMatcher.find()) {
                return generalMatcher.group(1);
            }
            
            // 方法4：查找数字版本号（兜底方案）
            // 在包含repeater关键字的响应中查找符合版本号模式的字符串
            if (responseContent.toLowerCase().contains("repeater")) {
                java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile(
                    "([0-9]+\\.[0-9]+\\.[0-9]+(?:-[a-zA-Z0-9]+)?)"
                );
                java.util.regex.Matcher numberMatcher = numberPattern.matcher(responseContent);
                if (numberMatcher.find()) {
                    return numberMatcher.group(1);
                }
                
                // 如果找到repeater但没有符合标准的版本号，返回默认版本
                return "1.0.0";
            }
            
            return "unknown";
            
        } catch (Exception e) {
            // 解析失败时记录日志但不影响注册流程
            return "unknown";
        }
    }

    @Override
    public RepeaterResult<Object> debugQueryAllModules() {
        try {
            ModuleInfoParams params = new ModuleInfoParams();
            params.setSize(1000); // 设置较大的数量以获取所有数据
            PageResult<ModuleInfoBO> result = query(params);
            
            if (result != null && result.isSuccess() && result.getData() != null) {
                List<Map<String, Object>> moduleList = new ArrayList<>();
                for (ModuleInfoBO module : result.getData()) {
                    Map<String, Object> moduleData = new HashMap<>();
                    moduleData.put("appName", module.getAppName());
                    moduleData.put("environment", module.getEnvironment());
                    moduleData.put("ip", module.getIp());
                    moduleData.put("port", module.getPort());
                    moduleData.put("status", module.getStatus());
                    moduleData.put("version", module.getVersion());
                    moduleData.put("gmtCreate", module.getGmtCreate());
                    moduleData.put("gmtModified", module.getGmtModified());
                    moduleList.add(moduleData);
                }
                return ResultHelper.success(moduleList);
            } else {
                return ResultHelper.success(new ArrayList<>());
            }
            
        } catch (Exception e) {
            return ResultHelper.fail("查询Module数据失败: " + e.getMessage());
        }
    }
}
