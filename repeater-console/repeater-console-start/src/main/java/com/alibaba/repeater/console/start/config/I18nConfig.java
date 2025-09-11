package com.alibaba.repeater.console.start.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.List;
import java.util.Locale;

/**
 * 国际化配置类
 * 
 * @author repeater
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {
    
    /**
     * 配置消息源
     * 这个Bean会被Spring Boot自动配置覆盖，所以实际使用application.properties中的配置
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);
        messageSource.setFallbackToSystemLocale(true);
        return messageSource;
    }
    
    /**
     * 配置LocaleResolver
     * 使用Cookie存储用户的语言偏好
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        resolver.setCookieName("repeater_locale");
        resolver.setCookieMaxAge(3600 * 24 * 30); // 30天
        resolver.setCookiePath("/");
        return resolver;
    }
    
    /**
     * 配置语言切换拦截器
     * 通过URL参数lang来切换语言，如?lang=en_US
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
    
    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    // ====== 以下是Spring 4.3.x WebMvcConfigurer接口要求的抽象方法实现 ======
    
    /**
     * 消息代码解析器 - Spring 4.3.x 必需实现
     * 返回null使用Spring默认的DefaultMessageCodesResolver
     */
    @Override
    public MessageCodesResolver getMessageCodesResolver() {
        return null;
    }

    /**
     * 验证器配置 - Spring 4.3.x 必需实现  
     * 返回null使用Spring默认验证器
     */
    @Override
    public Validator getValidator() {
        return null;
    }

    /**
     * 内容协商配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // 使用默认配置，不做任何自定义
    }

    /**
     * 异步支持配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 使用默认配置，不做任何自定义
    }

    /**
     * 默认servlet处理配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        // 使用默认配置，不做任何自定义
    }

    /**
     * 路径匹配配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 使用默认配置，不做任何自定义
    }

    /**
     * 视图控制器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 不添加任何视图控制器
    }

    /**
     * 资源处理器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用默认静态资源配置
    }

    /**
     * CORS配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 不配置CORS
    }

    /**
     * 视图解析器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        // 使用默认视图解析器配置
    }

    /**
     * 参数解析器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 不添加自定义参数解析器
    }

    /**
     * 返回值处理器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        // 不添加自定义返回值处理器
    }

    /**
     * 消息转换器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 使用默认消息转换器
    }

    /**
     * 扩展消息转换器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 不扩展消息转换器
    }

    /**
     * 格式化器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 不添加自定义格式化器
    }

    /**
     * 异常解析器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
        // 使用默认异常解析器
    }

    /**
     * 扩展异常解析器配置 - Spring 4.3.x 必需实现
     */
    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
        // 不扩展异常解析器
    }
}