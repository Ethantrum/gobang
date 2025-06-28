package com.example.gobang.server.handler.watch;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 观战者Session管理器：仅负责WebSocket会话注册、移除和消息推送。
 * 观战者身份、数据、权限等全部由RedisRoomManager分离结构管理。
 */
@Component
public class WatchSessionManager {
    // roomId -> (userId -> Set<session>)
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Set<WebSocketSession>>> roomWatchSessionsMap = new ConcurrentHashMap<>();

    /**
     * 注册观战者session（同一userId可多端观战）
     */
    public synchronized void registerWatchSession(Long roomId, Long userId, WebSocketSession newSession) {
        roomWatchSessionsMap.putIfAbsent(roomId, new ConcurrentHashMap<>());
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomWatchSessionsMap.get(roomId);
        userSessions.putIfAbsent(userId, new HashSet<>());
        Set<WebSocketSession> sessionSet = userSessions.get(userId);
        sessionSet.add(newSession);
    }

    /**
     * 移除一个观战者会话（断开连接时自动调用）
     */
    public synchronized void removeWatchSession(WebSocketSession session) {
        Object roomIdObj = session.getAttributes().get("roomId");
        Object userIdObj = session.getAttributes().get("userId");
        if (roomIdObj instanceof Long && userIdObj instanceof Long) {
            Long roomId = (Long) roomIdObj;
            Long userId = (Long) userIdObj;
            ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomWatchSessionsMap.get(roomId);
            if (userSessions != null) {
                Set<WebSocketSession> sessionSet = userSessions.get(userId);
                if (sessionSet != null) {
                    sessionSet.remove(session);
                    if (sessionSet.isEmpty()) {
                        userSessions.remove(userId);
                    }
                }
                if (userSessions.isEmpty()) {
                    roomWatchSessionsMap.remove(roomId);
                }
            }
        }
    }

    /**
     * 向指定房间的所有观战者会话广播消息
     */
    public synchronized void broadcastToRoom(Long roomId, Object message) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomWatchSessionsMap.get(roomId);
        if (userSessions == null) {
            return; // 没有观战者，直接返回
        }
        String messageJson = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(messageJson);
        userSessions.values().stream()
                .flatMap(Set::stream)
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException ignored) {}
                });
    }

    /**
     * 向指定观战者发送消息
     */
    public synchronized void sendToUser(Long roomId, Long userId, Object message) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomWatchSessionsMap.get(roomId);
        if (userSessions == null) return;
        Set<WebSocketSession> sessionSet = userSessions.get(userId);
        if (sessionSet == null) return;
        String messageJson = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(messageJson);
        sessionSet.stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException ignored) {}
                });
    }

    /**
     * 推送所有该房间观战者"房间无效"并断开连接。
     */
    public synchronized void notifyAndCloseAllRoomSessions(Long roomId, String reason) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomWatchSessionsMap.get(roomId);
        if (userSessions == null) return;
        String msg = com.alibaba.fastjson.JSON.toJSONString(com.example.gobang.common.result.WSResult.error(reason));
        for (Set<WebSocketSession> sessionSet : userSessions.values()) {
            for (WebSocketSession session : sessionSet) {
                try {
                    session.sendMessage(new TextMessage(msg));
                    session.close();
                } catch (Exception ignored) {}
            }
        }
        roomWatchSessionsMap.remove(roomId);
    }
} 