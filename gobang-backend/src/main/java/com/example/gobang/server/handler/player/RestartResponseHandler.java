package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class RestartResponseHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;

    @WSMessageHandler("restart_response")
    public void handleRestartResponse(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long fromUserId = data.getLong("fromUserId");
        if (roomId == null || fromUserId == null) {
            return;
        }
        boolean agree = data.getBoolean("agree");
        playerSessionManager.sendToUser(roomId, fromUserId, WSResult.restartResponse(agree));
    }
} 