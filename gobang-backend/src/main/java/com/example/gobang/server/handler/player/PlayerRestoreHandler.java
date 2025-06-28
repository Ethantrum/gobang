package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.player.PlayerSessionManager;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 玩家恢复Handler：处理玩家断线重连时的棋局恢复
 */
@Component
@RequiredArgsConstructor
public class PlayerRestoreHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerRestoreHandler.class);

    private final PlayerSessionManager playerSessionManager;
    private final RedisRoomManager redisRoomManager;

    @WSMessageHandler("player_restore")
    public void handlePlayerRestore(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("[玩家恢复] 收到playerRestore, roomId={}, userId={}", roomId, userId);
        
        if (roomId == null || userId == null) {
            return;
        }
        
        // 恢复玩家棋局状态
        JSONObject restoreData = redisRoomManager.getPlayerRestoreData(roomId.toString(), userId.toString());
        if (restoreData != null) {
            playerSessionManager.sendToUser(roomId, userId, WSResult.restore(restoreData));
            log.info("[玩家恢复] 发送恢复数据: {}", restoreData);
        } else {
            log.warn("[玩家恢复] 未找到恢复数据, roomId={}, userId={}", roomId, userId);
        }
    }
} 