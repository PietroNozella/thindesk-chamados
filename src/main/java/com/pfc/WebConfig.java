package com.pfc;

import com.pfc.security.AceiteInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AceiteInterceptor aceiteInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(aceiteInterceptor)
                .excludePathPatterns(
                        "/login", "/logout", "/aceite",
                        "/termos", "/privacidade",
                        "/esqueci-senha", "/redefinir-senha/**",
                        "/403", "/error",
                        "/css/**", "/js/**", "/images/**",
                        "/api/**");
    }
}
