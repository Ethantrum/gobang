package com.example.gobang.server.handler;

import com.alibaba.fastjson.JSON;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.player.PlayerSessionManager;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static com.example.gobang.common.constant.RoomUserRoleConstant.*;

/**
 * WebSocket主处理器：基于Redis分离结构实现房间、玩家、观战者的连接与权限管理。
 */
@Component
@Controller
public class MyWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(MyWebSocketHandler.class);

    @Autowired
    private WSDispatcher dispatcher;
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
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
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.connectionError("连接参数错误"))));
            session.close(CloseStatus.BAD_DATA.withReason("Invalid parameters"));
            return;
        }
        // 校验房间是否存在
        String roomKey = "room:" + roomId;
        if (roomId == null || userId == null || !Boolean.TRUE.equals(redisTemplate.hasKey(roomKey))) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.connectionError("非法连接：用户或房间不存在"))));
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthorized"));
            return;
        }
        // 校验玩家/观战者身份
        java.util.Map<Object, Object> playerInfo = redisTemplate.opsForHash().entries("room:" + roomId + ":player:" + userId);
        java.util.Map<Object, Object> watcherInfo = redisTemplate.opsForHash().entries("room:" + roomId + ":watcher:" + userId);
        if (playerInfo != null && !playerInfo.isEmpty()) {
            Object roleObj = playerInfo.get("role");
            role = roleObj == null ? ROLE_PLAYER : Byte.parseByte(roleObj.toString());
        } else if (watcherInfo != null && !watcherInfo.isEmpty()) {
            Object roleObj = watcherInfo.get("role");
            role = roleObj == null ? ROLE_WATCH : Byte.parseByte(roleObj.toString());
        } else {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.connectionError("非法连接：用户未在房间"))));
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthorized"));
            return;
        }
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("role", role);
        log.info("[WS] afterConnectionEstablished userId={}, roomId={}, role={}", userId, roomId, role);
        if (isPlayerRole(role)) {
            playerSessionManager.registerPlayerSession(roomId, userId, session);
        } else if (isWatchRole(role)) {
            watchSessionManager.registerWatchSession(roomId, userId, session);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        log.info("[WS] handleMessage sessionId={}, message={}", session.getId(), message.getPayload());
        String payload = message.getPayload().toString();
        com.alibaba.fastjson.JSONObject msg = com.alibaba.fastjson.JSON.parseObject(payload);
        String type = msg.getString("type");
        Byte role = (Byte) session.getAttributes().get("role");
        if (isPlayerOp(type) && !isPlayerRole(role)) {
            session.sendMessage(new TextMessage(com.alibaba.fastjson.JSON.toJSONString(WSResult.permissionError("观战者无法执行玩家操作：" + type))));
            log.warn("[WS] 拒绝观战者操作 type={}, sessionId={}", type, session.getId());
            return;
        }
        if (isWatchOp(type) && !isWatchRole(role)) {
            session.sendMessage(new TextMessage(com.alibaba.fastjson.JSON.toJSONString(WSResult.permissionError("玩家无法执行观战操作：" + type))));
            log.warn("[WS] 拒绝非观战者操作 type={}, sessionId={}", type, session.getId());
            return;
        }
        try {
            dispatcher.dispatch(session, message);
        } catch (Exception e) {
            log.error("[WS] 消息处理异常: {}", e.getMessage(), e);
            WSResult<String> errorResult = WSResult.systemError("消息处理失败: " + e.getMessage());
            String errorJson = JSON.toJSONString(errorResult);
            session.sendMessage(new TextMessage(errorJson));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("[WS] afterConnectionClosed sessionId={}, status={}", session.getId(), status);
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        Byte role = (Byte)session.getAttributes().get("role");
        if (roomId != null && userId != null && role != null) {
            if (isPlayerRole(role)) {
                playerSessionManager.removePlayerSession(session);
            } else if (isWatchRole(role)) {
                watchSessionManager.removeWatchSession(session);
            }
        }
        if (roomId != null) {
            dispatcher.dispatch(session, new TextMessage("{\"type\":\"leave\"}"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[WS] 传输错误: {}", exception.getMessage(), exception);
    }

    // 辅助方法
    private boolean isPlayerOp(String type) {
        return java.util.Set.of("move", "undo", "restart_request", "restart_response", "join", "leave", "playerRestore").contains(type);
    }
    private boolean isWatchOp(String type) {
        return java.util.Set.of("watchJoin", "watchLeave", "watchRestore").contains(type);
    }
    private boolean isPlayerRole(Byte role) {
        return role != null && (role == ROLE_PLAYER || role == ROLE_BLACK || role == ROLE_WHITE);
    }
    private boolean isWatchRole(Byte role) {
        return role != null && role == ROLE_WATCH;
    }
}
