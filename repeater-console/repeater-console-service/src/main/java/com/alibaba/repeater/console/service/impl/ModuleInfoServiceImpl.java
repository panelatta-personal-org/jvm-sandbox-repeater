package com.alibaba.repeater.console.service.impl;

import com.alibaba.jvm.sandbox.repeater.plugin.core.util.HttpUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.repeater.console.common.domain.ModuleInfoBO;
import com.alibaba.repeater.console.common.domain.ModuleStatus;
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
import java.util.Date;
import java.util.Locale;
import java.util.List;
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
        ModuleInfo moduleInfo = moduleInfoConverter.reconvert(params);
        moduleInfo.setGmtModified(new Date());
        moduleInfo.setGmtCreate(new Date());
        moduleInfoDao.save(moduleInfo);
        return ResultHelper.success(moduleInfoConverter.convert(moduleInfo));
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
            
            // 3. 进一步验证repeater模块是否存在
            String repeaterCheckUrl = String.format(
                "http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/detail?id=repeater", 
                params.getIp(), port);
            HttpUtil.Resp repeaterResp = HttpUtil.doGet(repeaterCheckUrl);
            
            if (!repeaterResp.isSuccess()) {
                return ResultHelper.fail(getMessage("error.repeater.not.found", 
                    params.getIp(), port));
            }
            
            // 4. 注册模块信息到数据库
            ModuleInfo moduleInfo = new ModuleInfo();
            moduleInfo.setAppName(params.getAppName());
            moduleInfo.setIp(params.getIp());
            moduleInfo.setPort(port); // 使用用户输入的端口
            moduleInfo.setStatus(ModuleStatus.ACTIVE.name());
            moduleInfo.setGmtCreate(new Date());
            moduleInfo.setGmtModified(new Date());
            
            // 5. 尝试获取模块版本信息（可选）
            try {
                String versionUrl = String.format(
                    "http://%s:%s/sandbox/default/module/http/sandbox-module-mgr/detail?id=repeater", 
                    params.getIp(), port);
                HttpUtil.Resp versionResp = HttpUtil.doGet(versionUrl);
                if (versionResp.isSuccess()) {
                    // 这里可以解析响应获取版本信息
                    // moduleInfo.setVersion(parseVersion(versionResp.getContent()));
                }
            } catch (Exception e) {
                // 获取版本信息失败不影响注册
            }
            
            moduleInfoDao.save(moduleInfo);
            
            return ResultHelper.success(getMessage("success.module.registered"), 
                getMessage("success.module.registered.detail", 
                    params.getAppName(), params.getIp(), port));
                
        } catch (Exception e) {
            return ResultHelper.fail(getMessage("error.registration.failed", e.getMessage()));
        }
    }

    @Override
    public RepeaterResult<String> reload(ModuleInfoParams params) {
        ModuleInfo moduleInfo = moduleInfoDao.findByAppNameAndIp(params.getAppName(), params.getIp());
        if (moduleInfo == null) {
            return ResultHelper.fail("data not exist");
        }
        HttpUtil.Resp resp = HttpUtil.doGet(String.format(reloadURI, moduleInfo.getIp(), moduleInfo.getPort()));
        return ResultHelper.fs(resp.isSuccess());
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
}
