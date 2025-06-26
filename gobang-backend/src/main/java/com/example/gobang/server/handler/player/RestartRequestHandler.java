package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class RestartRequestHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;

    @WSMessageHandler("restart_request")
    public void handleRestartRequest(WebSocketSession session, JSONObject data) {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            return;
        }
        // 只给对方player发送restart_request
        java.util.List<Long> playerIds = playerSessionManager.getPlayerIds(roomId);
        for (Long id : playerIds) {
            if (!id.equals(userId)) {
                playerSessionManager.sendToUser(roomId, id, com.example.gobang.common.result.WSResult.restartRequest());
            }
        }
    }
} 