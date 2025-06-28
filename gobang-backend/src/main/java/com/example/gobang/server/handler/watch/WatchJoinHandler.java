package com.example.gobang.server.handler.watch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;

/**
 * 观战加入处理器：基于Redis分离结构实现。
 * 观战者加入房间，注册session，写入观战者Hash，推送身份和棋盘状态。
 */
@Component
@RequiredArgsConstructor
public class WatchJoinHandler implements WebSocketMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(WatchJoinHandler.class);
    private final WatchSessionManager watchSessionManager;
    private final RedisRoomManager redisRoomManager;

    @WSMessageHandler("watchJoin")
    public void handleWatchJoin(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("[观战] 收到watchJoin, roomId={}, userId={}, data={}", roomId, userId, data);
        if (roomId == null || userId == null) {
            return;
        }
        // 1. 注册观战session
        watchSessionManager.registerWatchSession(roomId, userId, session);
        // 2. 写入观战者Hash，含nickname
        Map<Object, Object> userInfo = redisRoomManager.getRedisTemplate().opsForHash().entries("user:" + userId);
        Map<String, Object> watcherInfo = new HashMap<>();
        watcherInfo.put("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WATCH);
        watcherInfo.put("join_time", System.currentTimeMillis());
        watcherInfo.put("nickname", userInfo.getOrDefault("nickname", ""));
        redisRoomManager.addRoomWatcher(roomId.toString(), userId.toString(), watcherInfo);
        // 3. 推送观战身份
        JSONObject watchMsg = new JSONObject();
        watchMsg.put("isWatcher", true);
        JSONObject msg = new JSONObject();
        msg.put("type", "watch");
        msg.put("data", watchMsg);
        log.info("[观战] 推送身份消息: {}", msg);
        watchSessionManager.sendToUser(roomId, userId, msg);
        // 4. 推送棋盘最新状态（move类型）
        // 查找未结束的GameRecord（room_id匹配且end_time为空，start_time最大）
        Long foundGameId = null;
        Map<Object, Object> record = null;
        long maxGameId = 0L;
        Object maxGameIdObj = redisRoomManager.getRedisTemplate().opsForValue().get("game:id:incr");
        if (maxGameIdObj != null) {
            try { maxGameId = Long.parseLong(maxGameIdObj.toString()); } catch (Exception ignored) {}
        }
        long latestStartTime = -1;
        for (long gid = 1; gid <= maxGameId; gid++) {
            Map<Object, Object> game = redisRoomManager.getGameRecord(String.valueOf(gid));
            if (game == null || game.isEmpty()) continue;
            Object roomIdObj2 = game.get("room_id");
            Object endTimeObj = game.get("end_time");
            Object startTimeObj = game.get("start_time");
            if (roomIdObj2 != null && roomIdObj2.toString().equals(roomId.toString()) && (endTimeObj == null || endTimeObj.toString().isEmpty())) {
                long st = startTimeObj == null ? 0 : Long.parseLong(startTimeObj.toString());
                if (st > latestStartTime) {
                    foundGameId = gid;
                    record = game;
                    latestStartTime = st;
                }
            }
        }
        int[][] board = new int[15][15];
        int nextPlayer = 1;
        if (record != null) {
            List<Object> moves = redisRoomManager.getGameMoves(foundGameId.toString());
            for (Object moveObj : moves) {
                if (moveObj instanceof Map) {
                    Map mm = (Map) moveObj;
                    Object mx = mm.get("x");
                    Object my = mm.get("y");
                    Object player = mm.get("player");
                    if (mx != null && my != null && player != null) {
                        board[Integer.parseInt(mx.toString())][Integer.parseInt(my.toString())] = Integer.parseInt(player.toString());
                    }
                }
            }
            nextPlayer = (moves.size() % 2 == 0) ? 1 : 2;
        }
        JSONObject moveMsg = new JSONObject();
        moveMsg.put("board", board);
        moveMsg.put("nextPlayer", nextPlayer);
        log.info("[观战] 推送棋盘消息: {}", moveMsg);
        watchSessionManager.sendToUser(roomId, userId, WSResult.move(moveMsg));
    }
} 