package com.example.gobang.server.handler;

import com.example.gobang.pojo.dto.websocket.JoinDTO;
import com.example.gobang.pojo.dto.websocket.MoveDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
public class GameMessageHandler {

    @WSMessageHandler("join")
    public void handleJoin(WebSocketSession session, JoinDTO data) {
        // 处理玩家加入逻辑
    }

    @WSMessageHandler("move")
    public void handleMove(WebSocketSession session, MoveDTO data) {
        // 处理落子逻辑
    }
}