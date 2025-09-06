package com.yupi.yupicture.infrastructure.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
//        覆盖所有请求
        registry.addMapping("/**")
//               允许发送Cookie
                .allowCredentials(true)
//               放行哪些域名（必须使用patterns，否则*会和allowCredentials）
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");


    }

}
