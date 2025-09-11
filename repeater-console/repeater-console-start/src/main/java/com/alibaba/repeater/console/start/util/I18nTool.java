package com.alibaba.repeater.console.start.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类，用于Velocity模板中获取国际化文本
 * 
 * @author repeater
 */
@Component("i18n")
public class I18nTool {
    
    @Autowired
    private MessageSource messageSource;
    
    /**
     * 获取国际化消息
     * 
     * @param key 消息key
     * @return 国际化消息
     */
    public String get(String key) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, null, locale);
        } catch (Exception e) {
            // 如果找不到对应的消息，返回key本身
            return key;
        }
    }
    
    /**
     * 获取带参数的国际化消息
     * 
     * @param key 消息key
     * @param args 参数
     * @return 国际化消息
     */
    public String get(String key, Object... args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, args, locale);
        } catch (Exception e) {
            // 如果找不到对应的消息，返回key本身
            return key;
        }
    }
    
    /**
     * 获取当前语言
     * 
     * @return 当前语言代码
     */
    public String getCurrentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale.getLanguage() + "_" + locale.getCountry();
    }
    
    /**
     * 判断当前是否为中文环境
     * 
     * @return 是否中文
     */
    public boolean isChinese() {
        Locale locale = LocaleContextHolder.getLocale();
        return "zh".equals(locale.getLanguage());
    }
    
    /**
     * 判断当前是否为英文环境
     * 
     * @return 是否英文
     */
    public boolean isEnglish() {
        Locale locale = LocaleContextHolder.getLocale();
        return "en".equals(locale.getLanguage());
    }
}
