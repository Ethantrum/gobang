package com.example.gobang.server.controller.room;

import com.example.gobang.common.JWT.JwtUtils;
import com.example.gobang.common.result.Result;
import com.example.gobang.server.service.room.RoomCurrentService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomCurrentRoomController {
    @Autowired
    private RoomCurrentService roomCurrentService;

    @GetMapping("/api/room/current-room")
    public Result roomCurrentRoom(@RequestHeader("Authorization") String token) {
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
            return roomCurrentService.roomCurrentRoom(userId);
        } catch (Exception e) {
            return Result.error("用户请求时解析请求头失败: " + e.getMessage());
        }
    }
}
