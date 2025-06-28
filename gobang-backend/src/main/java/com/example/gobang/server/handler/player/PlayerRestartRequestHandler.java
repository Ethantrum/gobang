package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Set;

/**
 * 重开请求处理器：基于Redis分离结构实现。
 * 只允许房间内有效玩家发起重开请求，自动校验身份并推送。
 */
@Component
@RequiredArgsConstructor
public class PlayerRestartRequestHandler implements WebSocketMessageHandler {
    private final PlayerSessionManager playerSessionManager;
    private final RedisRoomManager redisRoomManager;

    @WSMessageHandler("restart_request")
    public void handleRestartRequest(WebSocketSession session, JSONObject data) {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long clientUserId = data.getLong("userId");
        if (userId == null || roomId == null) {
            return;
        }
        if (clientUserId == null || !userId.equals(clientUserId)) {
            playerSessionManager.sendToUser(roomId, userId, WSResult.permissionError("用户身份校验失败，禁止伪造userId发起重开"));
            return;
        }
        // 只统计player角色
        Set<Object> userIds = redisRoomManager.getRoomPlayerIds(roomId.toString());
        if (userIds == null || userIds.size() < 2) {
            playerSessionManager.sendToUser(roomId, userId, WSResult.gameStateError("房间内玩家不足，无法开始新对局。"));
            return;
        }
        // 只给对方player发送restart_request
        for (Object idObj : userIds) {
            Long id = Long.valueOf(idObj.toString());
            if (!id.equals(userId)) {
                playerSessionManager.sendToUser(roomId, id, WSResult.restartRequest(userId));
            }
        }
    }
} 