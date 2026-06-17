package com.campus.twohand.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局配置类
 * 1. 跨域配置
 * 2. 静态资源映射（让浏览器能访问 upload 目录）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 跨域配置（前端 5173 -> 后端 8080）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")   // 🔥 关键：用 patterns，不用 origins
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * ✅ 静态资源映射
     * 作用：
     * 浏览器访问：http://localhost:8080/upload/xxx.png
     * 实际读取：项目根目录下的 upload 文件夹
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:upload/");
    }
}
