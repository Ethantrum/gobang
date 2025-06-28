package com.example.gobang.server.handler.watch;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 观战恢复Handler：处理观战者断线重连时的棋局恢复
 */
@Component
public class WatchRestoreHandler {
    private static final Logger log = LoggerFactory.getLogger(WatchRestoreHandler.class);
    
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RedisRoomManager redisRoomManager;

    @WSMessageHandler("watchRestore")
    public void handleWatchRestore(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("[观战恢复] 收到watchRestore, roomId={}, userId={}", roomId, userId);
        
        if (roomId == null || userId == null) {
            return;
        }
        
        // 恢复观战者棋局状态
        JSONObject restoreData = redisRoomManager.getWatchRestoreData(roomId.toString(), userId.toString());
        if (restoreData != null) {
            watchSessionManager.sendToUser(roomId, userId, WSResult.restore(restoreData));
            log.info("[观战恢复] 发送恢复数据: {}", restoreData);
        } else {
            log.warn("[观战恢复] 未找到恢复数据, roomId={}, userId={}", roomId, userId);
        }
    }
} 