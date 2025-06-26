package com.example.gobang;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.security.SecureRandom;
import io.jsonwebtoken.security.Keys;

public class JwtKeyGenerator {

    public static void main(String[] args) {
        // 使用SecureRandom生成安全的随机字节（32字节=256位）
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);

        // 转换为SecretKey（可选，仅用于验证）
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);

        // 转换为Base64字符串（使用Java原生Base64编码）
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);  // 关键修改点

        // 输出结果
        System.out.println("生成的256位HS256密钥（Base64格式）:");
        System.out.println(base64Key);
        System.out.println("\n请将以下配置添加到application.properties:");
        System.out.println("jwt.secret=" + base64Key);
    }
}
