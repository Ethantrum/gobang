package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import com.example.gobang.server.service.GameRestartCacheService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 重开响应处理器：基于Redis分离结构实现。
 * 只允许房间内有效玩家同意重开，自动分配新黑白、创建新对局、推送消息。
 */
@Component
public class PlayerRestartResponseHandler implements WebSocketMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerRestartResponseHandler.class);
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RedisRoomManager redisRoomManager;
    @Autowired
    private GameRestartCacheService gameRestartCacheService;

    @WSMessageHandler("restart_response")
    public void handleRestartResponse(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long fromUserId = data.getLong("fromUserId");
        if (roomId == null || fromUserId == null) {
            return;
        }
        boolean agree = data.getBoolean("agree");
        playerSessionManager.sendToUser(roomId, fromUserId, WSResult.restartResponse(agree, fromUserId));
        if (!agree) {
            Long rejectUserId = (Long) session.getAttributes().get("userId");
            playerSessionManager.sendToUser(roomId, rejectUserId, WSResult.error("房间无效：您拒绝再来一局，已被移出房间"));
            playerSessionManager.removePlayerSession(session);
            return;
        }
        if (agree) {
            try {
                Long userId = (Long) session.getAttributes().get("userId");
                log.info("[再来一局] restart_response同意, roomId={}, userId={}, fromUserId={}", roomId, userId, fromUserId);
                // 1. 查找上一局GameRecord（最大gameId且room_id匹配）
                Long lastGameId = null;
                Long lastBlackId = null;
                Long lastWhiteId = null;
                
                // 优先从重开缓存中获取上一局信息
                Map<String, Object> lastGameInfo = gameRestartCacheService.getLastGameInfo(roomId);
                if (lastGameInfo != null) {
                    lastGameId = (Long) lastGameInfo.get("gameId");
                    lastBlackId = (Long) lastGameInfo.get("blackPlayerId");
                    lastWhiteId = (Long) lastGameInfo.get("whitePlayerId");
                    log.info("[再来一局] 从重开缓存找到上一局 - gameId: {}, blackId: {}, whiteId: {}", 
                            lastGameId, lastBlackId, lastWhiteId);
                } else {
                    log.info("[再来一局] 重开缓存中未找到上一局信息，从Redis查找 - roomId: {}", roomId);
                    // 从Redis查找当前进行中的对局
                    long maxGameId = 0L;
                    Object maxGameIdObj = redisRoomManager.getRedisTemplate().opsForValue().get("game:id:incr");
                    if (maxGameIdObj != null) {
                        try { maxGameId = Long.parseLong(maxGameIdObj.toString()); } catch (Exception ignored) {}
                    }
                    log.info("[再来一局] 当前最大gameId: {}", maxGameId);
                    for (long gid = maxGameId; gid >= 1; gid--) {
                        Map<Object, Object> game = redisRoomManager.getGameRecord(String.valueOf(gid));
                        if (game == null || game.isEmpty()) continue;
                        Object roomIdObj2 = game.get("room_id");
                        log.info("[再来一局] 检查gameId: {}, roomId: {}, 目标roomId: {}", gid, roomIdObj2, roomId);
                        if (roomIdObj2 != null && roomIdObj2.toString().equals(roomId.toString())) {
                            lastGameId = gid;
                            lastBlackId = game.get("black_id") == null ? null : Long.valueOf(game.get("black_id").toString());
                            lastWhiteId = game.get("white_id") == null ? null : Long.valueOf(game.get("white_id").toString());
                            log.info("[再来一局] 从Redis找到上一局 - gameId: {}, blackId: {}, whiteId: {}", 
                                    lastGameId, lastBlackId, lastWhiteId);
                            break;
                        }
                    }
                }
                
                if (lastBlackId == null || lastWhiteId == null) {
                    playerSessionManager.sendToUser(roomId, userId, WSResult.error("未找到上一局对局，无法重开"));
                    return;
                }
                Long newBlackId = lastWhiteId;
                Long newWhiteId = lastBlackId;
                // 查询房间所有玩家
                Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomId.toString());
                if (playerIds == null || playerIds.size() < 2) {
                    playerSessionManager.sendToUser(roomId, userId, WSResult.error("房间内玩家不足，无法开始新对局"));
                    return;
                }
                // 先全部重置为普通player
                for (Object uid : playerIds) {
                    Map<Object, Object> uinfo = redisRoomManager.getRoomPlayerInfo(roomId.toString(), uid.toString());
                    uinfo.put("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER);
                    redisRoomManager.addRoomPlayer(roomId.toString(), uid.toString(), convertToStringObjectMap(uinfo));
                }
                // 分配新黑白
                Map<Object, Object> blackInfo = redisRoomManager.getRoomPlayerInfo(roomId.toString(), newBlackId.toString());
                blackInfo.put("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_BLACK);
                redisRoomManager.addRoomPlayer(roomId.toString(), newBlackId.toString(), convertToStringObjectMap(blackInfo));
                Map<Object, Object> whiteInfo = redisRoomManager.getRoomPlayerInfo(roomId.toString(), newWhiteId.toString());
                whiteInfo.put("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WHITE);
                redisRoomManager.addRoomPlayer(roomId.toString(), newWhiteId.toString(), convertToStringObjectMap(whiteInfo));
                // 创建新对局
                Long newGameId = redisRoomManager.getNextGameId();
                Map<String, Object> newGame = new java.util.HashMap<>();
                newGame.put("room_id", roomId.toString());
                newGame.put("black_id", newBlackId.toString());
                newGame.put("white_id", newWhiteId.toString());
                newGame.put("start_time", System.currentTimeMillis());
                redisRoomManager.createGameRecord(newGameId.toString(), newGame);
                // 新增：重开时将房间状态设置为FULL
                Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomId.toString());
                if (roomInfo != null) {
                    roomInfo.put("status", com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_FULL);
                    redisRoomManager.createRoom(roomId.toString(), convertToStringObjectMap(roomInfo));
                }
                // 构造players数据，补充nickname（假设user:{userId} Hash有nickname字段）
                JSONObject startData = new JSONObject();
                startData.put("players", playerIds.stream().map(uid -> {
                    JSONObject ju = new JSONObject();
                    ju.put("userId", uid);
                    // 查询nickname
                    Map<Object, Object> userInfo = redisRoomManager.getRedisTemplate().opsForHash().entries("user:" + uid);
                    ju.put("nickname", userInfo.getOrDefault("nickname", ""));
                    ju.put("isBlack", uid.equals(newBlackId.toString()));
                    ju.put("isWhite", uid.equals(newWhiteId.toString()));
                    Map<Object, Object> uinfo = redisRoomManager.getRoomPlayerInfo(roomId.toString(), uid.toString());
                    Object role = uinfo.get("role");
                    ju.put("isWatcher", role != null && role.equals(com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WATCH));
                    return ju;
                }).collect(Collectors.toList()));
                startData.put("blackId", newBlackId);
                startData.put("whiteId", newWhiteId);
                startData.put("gameId", newGameId);
                log.info("[再来一局] 直接推送start消息: {}", startData);
                playerSessionManager.broadcastToRoom(roomId, WSResult.start(startData));
                watchSessionManager.broadcastToRoom(roomId, WSResult.start(startData));
                
                // 更新重开缓存，将当前局信息作为新的上一局信息
                gameRestartCacheService.cacheLastGameInfo(roomId, newGameId, newBlackId, newWhiteId);
            } catch (Exception e) {
                e.printStackTrace();
                playerSessionManager.broadcastToRoom(roomId, WSResult.error("再来一局失败：" + e.getMessage()));
            }
        }
    }

    private static Map<String, Object> convertToStringObjectMap(Map<Object, Object> map) {
        Map<String, Object> result = new java.util.HashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }
} 