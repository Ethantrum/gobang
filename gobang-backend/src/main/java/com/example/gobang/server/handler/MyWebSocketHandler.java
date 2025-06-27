package com.example.gobang.server.handler;

import com.alibaba.fastjson.JSON;
import com.example.gobang.common.constant.RoomUserRoleConstant;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.player.PlayerSessionManager;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.pojo.entity.RoomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

import static com.example.gobang.common.constant.RoomUserRoleConstant.*;

@Component
@Controller
public class MyWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(MyWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WSDispatcher dispatcher;
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RoomUserMapper roomUserMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket连接建立: " + session.getId());
        log.info("[WS] afterConnectionEstablished sessionId={}", session.getId());
        Long roomId = null;
        Long userId = null;
        byte role = ROLE_WATCH;
        try {
            String query = session.getUri().getQuery();
            String[] params = query.split("&");
            for (String param : params) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if (pair[0].equals("roomId")) roomId = Long.parseLong(pair[1]);
                    if (pair[0].equals("userId")) userId = Long.parseLong(pair[1]);
                }
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("连接参数错误"))));
            session.close(CloseStatus.BAD_DATA.withReason("Invalid parameters"));
            return;
        }
        log.info("[WS] afterConnectionEstablished userId={}, roomId={}", userId, roomId);
        RoomUser roomUser = roomUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RoomUser>()
                        .eq("user_id", userId)
                        .eq("room_id", roomId)
        );
        if (roomUser == null) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("非法连接：用户或房间不存在"))));
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthorized"));
            return;
        }
        role = roomUser.getRole();
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("role", role);
        log.info("[WS] afterConnectionEstablished role={}", role);
        if (ROLE_PLAYER.equals(role) || ROLE_BLACK.equals(role) || ROLE_WHITE.equals(role)) {
            log.info("[WS] registerPlayerSession called from afterConnectionEstablished");
            playerSessionManager.registerPlayerSession(roomId, userId, session);
        } else if (ROLE_WATCH.equals(role)) {
            log.info("[WS] registerWatchSession called from afterConnectionEstablished");
            watchSessionManager.registerWatchSession(roomId, userId, session);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        log.info("[WS] handleMessage sessionId={}, message={}", session.getId(), message.getPayload());
        try {
            dispatcher.dispatch(session, message);
        } catch (Exception e) {
            System.err.println("处理消息时发生异常: " + e.getMessage());
            e.printStackTrace();
            WSResult<String> errorResult = WSResult.error("消息处理失败: " + e.getMessage());
            String errorJson = JSON.toJSONString(errorResult);
            session.sendMessage(new TextMessage(errorJson));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket连接关闭: " + session.getId() + ", 状态: " + status);
        log.info("[WS] afterConnectionClosed sessionId={}, status={}", session.getId(), status);
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        Byte role = (Byte)session.getAttributes().get("role");
        if (roomId != null && userId != null && role != null) {
            if (ROLE_PLAYER.equals(role)||ROLE_BLACK.equals(role)||ROLE_WHITE.equals(role)) {
                playerSessionManager.removePlayerSession(session);
            } else if (ROLE_WATCH.equals(role)) {
                watchSessionManager.removeWatchSession(session);
            }
        }
        if (roomId != null) {
            dispatcher.dispatch(session, new TextMessage("{\"type\":\"leave\"}"));
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
        exception.printStackTrace();
    }
}
