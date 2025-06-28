package com.example.gobang.server.interceptor;

import com.example.gobang.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TokenInterceptor implements HandlerInterceptor {
    @Value("${jwt.secret}")
    private String jwtSecret;

    // 新增：读取 Token 校验开关配置
    @Value("${gobang.token.validation.enabled}")
    private boolean validationEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 如果开关关闭，直接放行所有请求
        if (!validationEnabled) {
            return true;
        }

        // 以下为原校验逻辑（开关开启时执行）
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(Result.error("未登录")));
            return false;
        }

        try {
            String realToken = token.substring(7);// 去掉 "Bearer " 前缀
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(realToken);
            return true;
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(Result.error("无效 Token 或已过期")));
            return false;
        }
    }
}