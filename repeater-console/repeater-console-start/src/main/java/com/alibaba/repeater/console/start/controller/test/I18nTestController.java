package com.alibaba.repeater.console.start.controller.test;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 国际化测试控制器
 * 用于测试国际化功能是否正常工作
 * 
 * @author repeater
 */
@Controller
@RequestMapping("/test")
public class I18nTestController {
    
    @RequestMapping("/i18n.htm")
    public String testI18n(Model model) {
        // 测试数据
        model.addAttribute("testData", "Test Data");
        return "test/i18n-test";
    }
}
