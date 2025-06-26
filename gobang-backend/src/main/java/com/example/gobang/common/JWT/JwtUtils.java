package com.example.gobang.common.JWT;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {

    @Getter
    private static String secret;

    @Value("${jwt.secret}") // 从配置文件中注入密钥
    public void setSecret(String secret) {
        JwtUtils.secret = secret;
    }

    // 过期时间（示例：1天，单位毫秒）
    private static final long EXPIRE_TIME = 86400_000;

    /**
     * 生成 JWT Token
     * @param userId 用户ID
     * @return Token 字符串
     */
    public static String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())  // 设置主题（用户ID）
                .setIssuedAt(new Date())  // 设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))  // 设置过期时间
                .signWith(SignatureAlgorithm.HS256, secret)  // 使用 HS256 算法和密钥签名
                .compact();  // 生成 Token
    }
}