package com.chronos.chronos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:1357,https://time-capsule-snowy.vercel.app}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Range", "Accept-Ranges", "Content-Length", "Content-Type", "ETag",
                                "Last-Modified")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}