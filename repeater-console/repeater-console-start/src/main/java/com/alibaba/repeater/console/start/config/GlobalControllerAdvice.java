package com.alibaba.repeater.console.start.config;

import com.alibaba.repeater.console.start.util.I18nTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全局Controller增强
 * 用于向所有Controller的Model中注入公共属性
 * 
 * @author repeater
 */
@ControllerAdvice
public class GlobalControllerAdvice {
    
    @Autowired
    private I18nTool i18nTool;
    
    /**
     * 将国际化工具注入到所有的Model中
     * 使得在Velocity模板中可以通过$i18n访问
     */
    @ModelAttribute
    public void addI18nTool(Model model) {
        model.addAttribute("i18n", i18nTool);
    }
}
