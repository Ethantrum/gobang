package com.example.gobang.server.controller.user;


import com.example.gobang.common.JWT.JwtUtils;
import com.example.gobang.common.result.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserLogoutController {

    public Result logout(@RequestHeader("Authorization") String token) {
        try {
            // 去掉"Bearer "前缀
            String realToken = token.substring(7);
            // 解析token获取用户ID
            Claims claims = Jwts.parser()
                    .setSigningKey(JwtUtils.getSecret())
                    .parseClaimsJws(realToken)
                    .getBody();
            Long userId = Long.parseLong(claims.getSubject());

            // 调用service处理退出逻辑
            //return userService.logout(userId);
            return Result.success("退出成功");
        } catch (Exception e) {
            return Result.error("用户请求退出时解析请求头失败: " + e.getMessage());
        }
    }
}
