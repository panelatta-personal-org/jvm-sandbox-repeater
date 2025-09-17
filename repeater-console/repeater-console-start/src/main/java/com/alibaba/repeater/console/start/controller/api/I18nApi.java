package com.alibaba.repeater.console.start.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.ResourceBundle;

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
     * 动态加载资源文件中的所有消息键，确保前端可以访问任何定义的消息
     */
    private Map<String, String> loadMessages(Locale locale) {
        Map<String, String> messages = new HashMap<>();
        
        try {
            // 动态获取资源包中的所有键
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
            Enumeration<String> keys = bundle.getKeys();
            
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                try {
                    String message = messageSource.getMessage(key, null, locale);
                    messages.put(key, message);
                } catch (Exception e) {
                    // 如果MessageSource无法获取消息，直接从ResourceBundle获取
                    try {
                        String message = bundle.getString(key);
                        messages.put(key, message);
                    } catch (Exception ex) {
                        // 最后的fallback，使用key本身
                        messages.put(key, key);
                    }
                }
            }
        } catch (Exception e) {
            // 如果ResourceBundle加载失败，记录错误并返回空Map
            System.err.println("Failed to load ResourceBundle for locale: " + locale + ", error: " + e.getMessage());
        }
        
        return messages;
    }
}
