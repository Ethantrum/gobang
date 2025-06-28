package com.example.gobang.server.handler.watch;

import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;

/**
 * 观战离开处理器：基于Redis分离结构实现。
 * 观战者离开房间，自动处理房主转让、房间解散、成员清理等。
 */
@Component
public class WatchLeaveHandler implements WebSocketMessageHandler {
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RedisRoomManager redisRoomManager;

    @WSMessageHandler("watchLeave")
    public void handleWatchLeave(WebSocketSession session, Object data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId == null || userId == null) {
            return;
        }
        String roomIdStr = roomId.toString();
        String userIdStr = userId.toString();
        Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomIdStr);
        if (roomInfo != null && roomInfo.get("owner_id") != null && roomInfo.get("owner_id").toString().equals(userIdStr)) {
            // 当前用户是房主，转移房主或解散房间
            Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomIdStr);
            Set<Object> watcherIds = redisRoomManager.getRoomWatcherIds(roomIdStr);
            List<String> leftPlayers = new ArrayList<>();
            if (playerIds != null) {
                for (Object uid : playerIds) if (!uid.toString().equals(userIdStr)) leftPlayers.add(uid.toString());
            }
            String newOwnerId = null;
            if (!leftPlayers.isEmpty()) {
                newOwnerId = leftPlayers.get(0);
            } else if (watcherIds != null && !watcherIds.isEmpty()) {
                for (Object uid : watcherIds) {
                    if (!uid.toString().equals(userIdStr)) {
                        newOwnerId = uid.toString();
                        break;
                    }
                }
            }
            if (newOwnerId != null) {
                roomInfo.put("owner_id", newOwnerId);
                redisRoomManager.createRoom(roomIdStr, convertToStringObjectMap(roomInfo));
            } else {
                redisRoomManager.deleteRoom(roomIdStr);
            }
        } else {
            // 不是房主则删除观战者信息
            redisRoomManager.removeRoomWatcher(roomIdStr, userIdStr);
        }
        // 移除观战session并断开
        watchSessionManager.removeWatchSession(session);
        try {
            session.close();
        } catch (Exception ignored) {}
    }

    private static Map<String, Object> convertToStringObjectMap(Map<Object, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }
} 