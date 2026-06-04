package com.deska.evolvelog.config;

import com.deska.evolvelog.i18n.LocaleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LocaleInterceptor localeInterceptor;

    public WebMvcConfig(LocaleInterceptor localeInterceptor) {
        this.localeInterceptor = localeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeInterceptor).addPathPatterns("/api/**");
    }
}
