package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Component
public class RestartRequestHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;

    @Autowired
    private RoomUserMapper roomUserMapper;

    @WSMessageHandler("restart_request")
    public void handleRestartRequest(WebSocketSession session, JSONObject data) {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long clientUserId = data.getLong("userId");
        if (userId == null || roomId == null) {
            return;
        }
        if (clientUserId == null || !userId.equals(clientUserId)) {
            playerSessionManager.sendToUser(roomId, userId, WSResult.error("用户身份校验失败，禁止伪造userId发起重开"));
            return;
        }
        // 新增：校验房间内玩家数量（用内存hashmap）
        java.util.List<Long> playerIds = playerSessionManager.getPlayerIds(roomId);
        if (playerIds.size() < 2) {
            playerSessionManager.sendToUser(roomId, userId, WSResult.error("房间内玩家不足，无法开始新对局。"));
            return;
        }
        // 只给对方player发送restart_request
        for (Long id : playerIds) {
            if (!id.equals(userId)) {
                playerSessionManager.sendToUser(roomId, id, com.example.gobang.common.result.WSResult.restartRequest(userId));
            }
        }
    }
} 