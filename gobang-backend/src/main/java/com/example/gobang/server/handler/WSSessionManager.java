package com.example.gobang.server.handler;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WSSessionManager {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 添加一个新的会话
     * @param session WebSocket会话
     */
    public void add(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    /**
     * 移除一个会话
     * @param session WebSocket会话
     */
    public void remove(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    /**
     * 向指定房间的所有会话广播消息
     * @param roomId 房间ID
     * @param message 要发送的消息对象
     */
    public synchronized void broadcastToRoom(Long roomId, Object message) {
        String messageJson = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(messageJson);

        sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .filter(session -> roomId.equals(session.getAttributes().get("roomId")))
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        System.err.println("向会话 " + session.getId() + " 发送消息时出错: " + e.getMessage());
                    }
                });
    }

    /**
     * 向指定用户发送消息
     * @param userId 用户ID
     * @param message 要发送的消息对象
     */
    public synchronized void sendToUser(Long userId, Object message) {
        String messageJson = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(messageJson);
        sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .filter(session -> userId.equals(session.getAttributes().get("userId")))
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        System.err.println("向用户 " + userId + " 发送消息时出错: " + e.getMessage());
                    }
                });
    }
} 