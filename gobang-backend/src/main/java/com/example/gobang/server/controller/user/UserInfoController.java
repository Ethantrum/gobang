package com.example.gobang.server.controller.user;

import com.example.gobang.common.JWT.JwtUtils;
import com.example.gobang.common.result.Result;
import com.example.gobang.server.service.user.UserInfoService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserInfoController {
    private final UserInfoService userInfoService;

    @GetMapping("/api/user/info")
    public Result getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            // 去掉"Bearer "前缀
            String realToken = token.substring(7);
            // 解析token获取用户ID
            Claims claims = Jwts.parser()
                    .setSigningKey(JwtUtils.getSecret())
                    .parseClaimsJws(realToken)
                    .getBody();
            Long userId = Long.parseLong(claims.getSubject());

            // 调用service获取用户信息
            return userInfoService.getUserInfo(userId);
        } catch (Exception e) {
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
}
