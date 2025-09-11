package com.alibaba.repeater.console.start.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 国际化API
 * 提供前端JavaScript访问国际化消息的接口
 * 
 * @author repeater
 */
@RestController
@RequestMapping("/api/i18n")
public class I18nApi {
    
    @Autowired
    private MessageSource messageSource;
    
    /**
     * 获取当前语言的所有消息
     * 
     * @param locale 语言代码，如zh_CN, en_US
     * @return 消息键值对
     */
    @GetMapping("/messages")
    public Map<String, String> getMessages(@RequestParam(required = false) String locale) {
        Locale targetLocale = parseLocale(locale);
        Map<String, String> messages = loadMessages(targetLocale);
        return messages;
    }
    
    /**
     * 获取指定key的消息
     * 
     * @param key 消息key
     * @param locale 语言代码
     * @param args 消息参数
     * @return 国际化消息
     */
    @GetMapping("/message/{key}")
    public Map<String, String> getMessage(@PathVariable String key,
                                          @RequestParam(required = false) String locale,
                                          @RequestParam(required = false) List<String> args) {
        Locale targetLocale = parseLocale(locale);
        String message;
        try {
            if (args != null && !args.isEmpty()) {
                message = messageSource.getMessage(key, args.toArray(), targetLocale);
            } else {
                message = messageSource.getMessage(key, null, targetLocale);
            }
        } catch (Exception e) {
            message = key;
        }
        
        Map<String, String> result = new HashMap<>();
        result.put("key", key);
        result.put("message", message);
        result.put("locale", targetLocale.toString());
        return result;
    }
    
    /**
     * 获取当前语言
     * 
     * @return 当前语言信息
     */
    @GetMapping("/current-locale")
    public Map<String, String> getCurrentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> result = new HashMap<>();
        result.put("locale", locale.toString());
        result.put("language", locale.getLanguage());
        result.put("country", locale.getCountry());
        result.put("displayName", locale.getDisplayName());
        return result;
    }
    
    /**
     * 获取支持的语言列表
     * 
     * @return 语言列表
     */
    @GetMapping("/supported-locales")
    public List<Map<String, String>> getSupportedLocales() {
        List<Map<String, String>> locales = new ArrayList<>();
        
        Map<String, String> zhCN = new HashMap<>();
        zhCN.put("locale", "zh_CN");
        zhCN.put("displayName", "中文");
        zhCN.put("flag", "🇨🇳");
        locales.add(zhCN);
        
        Map<String, String> enUS = new HashMap<>();
        enUS.put("locale", "en_US");
        enUS.put("displayName", "English");
        enUS.put("flag", "🇺🇸");
        locales.add(enUS);
        
        return locales;
    }
    
    /**
     * 解析语言代码
     */
    private Locale parseLocale(String locale) {
        if (locale == null || locale.isEmpty()) {
            return LocaleContextHolder.getLocale();
        }
        
        String[] parts = locale.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(parts[0], parts[1], parts[2]);
        }
    }
    
    /**
     * 加载指定语言的所有消息
     * 这里只加载常用的消息，避免一次性加载过多
     */
    private Map<String, String> loadMessages(Locale locale) {
        Map<String, String> messages = new HashMap<>();
        
        // 预定义需要加载的消息key列表
        String[] keys = {
            // 导航菜单
            "nav.online.traffic", "nav.config.management", "nav.online.module",
            // 页面标题
            "title.online.traffic", "title.config.management", "title.online.module",
            "title.call.detail", "title.replay.result", "title.config.add", "title.config.edit",
            "title.config.detail", "title.module.list", "title.regress", "title.batch.replay",
            // 按钮
            "button.query", "button.add", "button.add.config", "button.edit",
            "button.delete", "button.detail", "button.replay", "button.execute",
            "button.cancel", "button.confirm", "button.submit", "button.save",
            "button.batch.replay", "button.push", "button.activate", "button.install.module",
            "button.refresh", "button.active", "button.select.all", "button.back", "button.reset",
            // 表单标签
            "label.app.name", "label.environment", "label.trace.id", "label.machine",
            "label.ip", "label.entrance", "label.status", "label.time",
            "label.create.time", "label.modify.time", "label.heartbeat.time",
            "label.config.info", "label.operation", "label.host", "label.port",
            "label.version", "label.mock",
            // 表格列头
            "table.app.name", "table.environment", "table.entrance", "table.trace.id",
            "table.status", "table.time", "table.operation", "table.config.info",
            "table.create.time", "table.modify.time", "table.heartbeat.time",
            "table.host", "table.port", "table.version", "table.machine", "table.select.all",
            // 表单字段
            "form.config.content", "form.config.model", "form.module.name", 
            "form.ip.address", "form.machine.ip", "form.scenario.name",
            // 状态信息
            "status.active", "status.inactive", "status.online", "status.offline",
            "status.success", "status.failed",
            // 提示信息
            "msg.no.config", "msg.no.machine", "msg.no.data", "msg.network.error",
            "msg.replay.success", "msg.loading", "msg.success", "msg.failed",
            "msg.confirm.delete", "msg.confirm.operation", "msg.no.data.recorded",
            "msg.no.replay.machine", "msg.install.success", "msg.activate.success",
            "msg.push.success", "msg.select.records.first", "msg.confirm.batch.replay",
            "msg.install.module.confirm", "msg.activate.module.confirm",
            // 页面元素
            "page.home", "page.breadcrumb", "page.new.badge", "page.call.list",
            "page.recorded.data", "page.no.available.machine", "page.please.select.machine",
            // 模态框标题
            "modal.title.tip", "modal.title.start.replay", "modal.title.install.module",
            "modal.title.confirm", "modal.title.batch.replay", "modal.title.select.machine",
            // 其他
            "other.open", "other.close", "other.yes", "other.no", "other.all", "other.none",
            "other.loading", "other.processing"
        };
        
        for (String key : keys) {
            try {
                String message = messageSource.getMessage(key, null, locale);
                messages.put(key, message);
            } catch (Exception e) {
                // 如果找不到消息，使用key本身
                messages.put(key, key);
            }
        }
        
        return messages;
    }
}
