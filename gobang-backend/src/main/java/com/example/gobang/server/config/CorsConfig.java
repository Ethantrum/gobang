package com.example.gobang.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局跨域配置类，用于解决前端与后端的跨域请求问题。
 * 通过注册 CORS 过滤器，允许指定域名的前端应用访问后端接口，
 * 并配置允许的请求方法、请求头及凭证信息。
 */
@Configuration
public class CorsConfig {

    // 从配置文件读取前端域名（开发环境默认 http://localhost:8082）
    @Value("${gobang.frontend.origin}")
    private String requestURL;

    /**
     * 创建并配置全局 CORS 过滤器，应用于所有 HTTP 请求。
     *
     * @return 配置好的 CORS 过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 允许的前端域名（源），从配置文件读取，生产环境只需修改配置即可
        config.addAllowedOrigin(requestURL);

        // 允许的 HTTP 请求方法（* 表示所有方法，生产环境建议指定具体方法如 GET, POST）
        config.addAllowedMethod("*");

        // 允许的请求头（* 表示所有头，生产环境建议指定具体头如 Content-Type, Authorization）
        config.addAllowedHeader("*");

        // 允许携带 Cookie 等凭证信息（启用时 allowedOrigins 不能使用 *）
        config.setAllowCredentials(true);

        // 创建 CORS 配置源，将配置应用到所有接口路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 返回配置好的 CORS 过滤器
        return new CorsFilter(source);
    }
}