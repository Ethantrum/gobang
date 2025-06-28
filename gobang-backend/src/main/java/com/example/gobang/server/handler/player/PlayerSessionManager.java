package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_FULL;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_READY;
import static com.example.gobang.common.constant.RoomUserRoleConstant.*;

import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import com.example.gobang.server.service.GameArchiveService;

/**
 * 玩家Session管理器：基于Redis分离结构实现房间、玩家、观战者、对局等管理。
 */
@Component
public class PlayerSessionManager {
    private static final Logger log = LoggerFactory.getLogger(PlayerSessionManager.class);
    // roomId -> (userId -> Set<session>)
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Set<WebSocketSession>>> roomUserSessionsMap = new ConcurrentHashMap<>();

    private final WatchSessionManager watchSessionManager;
    private final RedisRoomManager redisRoomManager;
    private final GameArchiveService gameArchiveService;

    public PlayerSessionManager(WatchSessionManager watchSessionManager, RedisRoomManager redisRoomManager, GameArchiveService gameArchiveService) {
        this.watchSessionManager = watchSessionManager;
        this.redisRoomManager = redisRoomManager;
        this.gameArchiveService = gameArchiveService;
    }

    /**
     * 注册玩家session，只允许一个session，多余的直接拒绝。
     * 满2人自动分配棋子并开始游戏。
     */
    public synchronized void registerPlayerSession(Long roomId, Long userId, WebSocketSession newSession) {
        log.info("[registerPlayerSession] roomId={}, userId={}, sessionId={}", roomId, userId, newSession.getId());
        roomUserSessionsMap.putIfAbsent(roomId, new ConcurrentHashMap<>());
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        userSessions.putIfAbsent(userId, new HashSet<>());
        Set<WebSocketSession> sessionSet = userSessions.get(userId);
        if (!sessionSet.isEmpty()) {
            log.warn("[registerPlayerSession] userId={} already has session, closing newSession", userId);
            try { newSession.close(); } catch (IOException ignored) {}
            return;
        }
        sessionSet.add(newSession);
        log.info("[registerPlayerSession] session registered, current userSessions: {}", userSessions);
        // 检查房间内player数量，满2人则分配棋子并开始游戏
        if (getPlayerCount(roomId) == 2) {
            log.info("[registerPlayerSession] roomId={} 满2人，开始分配棋子并开始游戏", roomId);
            startGameForTwoPlayers(roomId, userSessions);
        }
    }

    /**
     * 玩家彻底退出房间（无论被踢、主动离开、断线等）
     */
    public synchronized void playerQuitRoom(Long roomId, Long userId) {
        // 关闭所有该玩家的session
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        if (userSessions != null) {
            Set<WebSocketSession> sessionSet = userSessions.get(userId);
            if (sessionSet != null) {
                for (WebSocketSession session : new HashSet<>(sessionSet)) {
                    try { session.close(); } catch (IOException ignored) {}
                }
            }
        }
        // 统一清理
        removePlayerSessionById(roomId, userId);
    }

    /**
     * 通过roomId和userId移除玩家，包含所有redis和本地清理、房主转让、房间状态等
     */
    public synchronized void removePlayerSessionById(Long roomId, Long userId) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        if (userSessions != null) {
            userSessions.remove(userId);
            if (userSessions.isEmpty()) roomUserSessionsMap.remove(roomId);
        }
        // --- Redis实现 ---
        String roomIdStr = roomId.toString();
        String userIdStr = userId.toString();
        redisRoomManager.removeRoomPlayer(roomIdStr, userIdStr);
        redisRoomManager.removeUserCurrentRoom(userIdStr);
        Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomIdStr);
        if (roomInfo == null || roomInfo.isEmpty()) return;
        Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomIdStr);
        Set<Object> watcherIds = redisRoomManager.getRoomWatcherIds(roomIdStr);
        // 房主转让逻辑
        Object ownerIdObj = roomInfo.get("owner_id");
        if (ownerIdObj != null && ownerIdObj.toString().equals(userIdStr)) {
            List<String> leftPlayers = new ArrayList<>();
            if (playerIds != null) {
                for (Object uid : playerIds) if (!uid.toString().equals(userIdStr)) leftPlayers.add(uid.toString());
            }
            String newOwnerId = null;
            if (!leftPlayers.isEmpty()) {
                newOwnerId = leftPlayers.get(0);
            } else if (watcherIds != null && !watcherIds.isEmpty()) {
                for (Object uid : watcherIds) {
                    newOwnerId = uid.toString();
                    break;
                }
            }
            if (newOwnerId != null) {
                roomInfo.put("owner_id", newOwnerId);
                redisRoomManager.createRoom(roomIdStr, (Map) roomInfo);
            } else {
                notifyAndCloseAllRoomSessions(roomId, "房间已解散或无效");
                watchSessionManager.notifyAndCloseAllRoomSessions(roomId, "房间已解散或无效");
                redisRoomManager.deleteRoom(roomIdStr);
                return;
            }
        }
        // 判定胜负与房间状态（只统计player）
        List<String> leftPlayers = new ArrayList<>();
        if (playerIds != null) {
            for (Object uid : playerIds) leftPlayers.add(uid.toString());
        }
        if (leftPlayers.size() == 1) {
            Long winnerId = Long.valueOf(leftPlayers.get(0));
            Long foundGameId = null;
            Map<Object, Object> foundGame = null;
            long maxGameId = 0L;
            Object maxGameIdObj = redisRoomManager.getRedisTemplate().opsForValue().get("game:id:incr");
            if (maxGameIdObj != null) {
                try { maxGameId = Long.parseLong(maxGameIdObj.toString()); } catch (Exception ignored) {}
            }
            for (long gid = 1; gid <= maxGameId; gid++) {
                Map<Object, Object> game = redisRoomManager.getGameRecord(String.valueOf(gid));
                if (game == null || game.isEmpty()) continue;
                Object roomIdObj2 = game.get("room_id");
                Object endTimeObj = game.get("end_time");
                if (roomIdObj2 != null && roomIdObj2.toString().equals(roomIdStr) && (endTimeObj == null || endTimeObj.toString().isEmpty())) {
                    foundGameId = gid;
                    foundGame = game;
                    break;
                }
            }
            if (foundGameId != null && foundGame != null) {
                foundGame.put("winner", winnerId.toString());
                foundGame.put("end_time", System.currentTimeMillis());
                redisRoomManager.createGameRecord(foundGameId.toString(), (Map) foundGame);
                
                // TODO: 游戏结束归档 - 调用GameArchiveService.archiveGame(foundGameId)将游戏数据归档到MySQL
                gameArchiveService.archiveGame(roomId, foundGameId);
                
                JSONObject resultData = new JSONObject();
                resultData.put("winner", winnerId);
                broadcastToRoom(roomId, WSResult.result(resultData));
            }
            roomInfo.put("status", ROOM_STATUS_READY);
            redisRoomManager.createRoom(roomIdStr, (Map) roomInfo);
        } else if (leftPlayers.size() < 2) {
            roomInfo.put("status", ROOM_STATUS_READY);
            redisRoomManager.createRoom(roomIdStr, (Map) roomInfo);
        }
    }

    // 原有removePlayerSession(WebSocketSession session)内部调用新方法
    public synchronized void removePlayerSession(WebSocketSession session) {
        Object roomIdObj = session.getAttributes().get("roomId");
        Object userIdObj = session.getAttributes().get("userId");
        log.info("[removePlayerSession] roomId={}, userId={}, sessionId={}", roomIdObj, userIdObj, session.getId());
        if (!(roomIdObj instanceof Long) || !(userIdObj instanceof Long)) return;
        Long roomId = (Long) roomIdObj;
        Long userId = (Long) userIdObj;
        removePlayerSessionById(roomId, userId);
    }

    /**
     * 向房间所有玩家广播消息。
     */
    public synchronized void broadcastToRoom(Long roomId, Object message) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        if (userSessions == null) return;
        String msg = message instanceof String ? (String) message : JSON.toJSONString(message);
        for (Set<WebSocketSession> sessionSet : userSessions.values()) {
            for (WebSocketSession session : sessionSet) {
                try { session.sendMessage(new TextMessage(msg)); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 向指定玩家发送消息。
     */
    public synchronized void sendToUser(Long roomId, Long userId, Object message) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        if (userSessions == null) return;
        Set<WebSocketSession> sessionSet = userSessions.get(userId);
        if (sessionSet == null) return;
        String msg = message instanceof String ? (String) message : JSON.toJSONString(message);
        for (WebSocketSession session : sessionSet) {
            try { session.sendMessage(new TextMessage(msg)); } catch (IOException ignored) {}
        }
    }

    /**
     * 获取房间玩家数量。
     */
    private int getPlayerCount(Long roomId) {
        Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomId.toString());
        return playerIds == null ? 0 : playerIds.size();
    }

    /**
     * 满2人自动分配黑白棋、创建对局、推送WSResult.start。
     */
    private void startGameForTwoPlayers(Long roomId, ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions) {
        List<Long> playerIds = new ArrayList<>(userSessions.keySet());
        if (playerIds.size() != 2) return;
        Long blackId = playerIds.get(0);
        Long whiteId = playerIds.get(1);
        // 更新成员Hash角色
        Map<Object, Object> blackInfo = redisRoomManager.getRoomPlayerInfo(roomId.toString(), blackId.toString());
        if (blackInfo != null) {
            blackInfo.put("role", ROLE_BLACK);
            redisRoomManager.addRoomPlayer(roomId.toString(), blackId.toString(), convertToStringObjectMap(blackInfo));
        }
        Map<Object, Object> whiteInfo = redisRoomManager.getRoomPlayerInfo(roomId.toString(), whiteId.toString());
        if (whiteInfo != null) {
            whiteInfo.put("role", ROLE_WHITE);
            redisRoomManager.addRoomPlayer(roomId.toString(), whiteId.toString(), convertToStringObjectMap(whiteInfo));
        }
        // 游戏开始时设置房间状态为FULL
        Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomId.toString());
        if (roomInfo != null) {
            roomInfo.put("status", ROOM_STATUS_FULL);
            redisRoomManager.createRoom(roomId.toString(), convertToStringObjectMap(roomInfo));
        }
        // 创建对局记录
        Long newGameId = redisRoomManager.getNextGameId();
        Map<String, Object> gameInfo = new HashMap<>();
        gameInfo.put("room_id", roomId.toString());
        gameInfo.put("black_id", blackId.toString());
        gameInfo.put("white_id", whiteId.toString());
        gameInfo.put("start_time", System.currentTimeMillis());
        redisRoomManager.createGameRecord(newGameId.toString(), gameInfo);
        // 查询玩家信息
        List<JSONObject> players = new ArrayList<>();
        for (Long pid : playerIds) {
            Map<Object, Object> uinfo = redisRoomManager.getRoomPlayerInfo(roomId.toString(), pid.toString());
            JSONObject ju = new JSONObject();
            ju.put("userId", pid);
            ju.put("nickname", uinfo.getOrDefault("nickname", ""));
            ju.put("isBlack", pid.equals(blackId));
            ju.put("isWhite", pid.equals(whiteId));
            players.add(ju);
        }
        // 构造并广播开始消息
        JSONObject startData = new JSONObject();
        startData.put("players", players);
        startData.put("blackId", blackId);
        startData.put("whiteId", whiteId);
        startData.put("gameId", newGameId);
        broadcastToRoom(roomId, WSResult.start(startData));
    }

    private static Map<String, Object> convertToStringObjectMap(Map<Object, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    private boolean isPlayer(byte role) {
        return role == ROLE_PLAYER || role == ROLE_BLACK || role == ROLE_WHITE;
    }

    /**
     * 推送所有该房间玩家"房间无效"并断开连接。
     */
    public synchronized void notifyAndCloseAllRoomSessions(Long roomId, String reason) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        if (userSessions == null) return;
        
        // 检查Redis中是否有kick消息
        Object kickReason = redisRoomManager.getRedisTemplate().opsForValue().get("room:" + roomId + ":kick_reason");
        String finalReason = kickReason != null ? kickReason.toString() : reason;
        
        // 发送kick消息而不是error消息
        String msg = com.alibaba.fastjson.JSON.toJSONString(com.example.gobang.common.result.WSResult.<String>kick(finalReason));
        for (Set<WebSocketSession> sessionSet : userSessions.values()) {
            for (WebSocketSession session : sessionSet) {
                try {
                    session.sendMessage(new TextMessage(msg));
                    session.close();
                } catch (Exception ignored) {}
            }
        }
        roomUserSessionsMap.remove(roomId);
    }

} 