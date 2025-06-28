package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import java.io.IOException;
import java.util.*;

/**
 * 悔棋处理器：基于Redis分离结构实现。
 * 只允许当前对局玩家悔棋，撤销最后一步落子，自动同步棋盘。
 */
@Component
public class PlayerUndoHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RedisRoomManager redisRoomManager;

    @WSMessageHandler("undo")
    public void handleUndo(WebSocketSession session, JSONObject data) {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            return;
        }
        // 1. 查找未结束的对局
        Long foundGameId = null;
        Map<Object, Object> record = null;
        long maxGameId = 0L;
        Object maxGameIdObj = redisRoomManager.getRedisTemplate().opsForValue().get("game:id:incr");
        if (maxGameIdObj != null) {
            try { maxGameId = Long.parseLong(maxGameIdObj.toString()); } catch (Exception ignored) {}
        }
        for (long gid = maxGameId; gid >= 1; gid--) {
            Map<Object, Object> game = redisRoomManager.getGameRecord(String.valueOf(gid));
            if (game == null || game.isEmpty()) continue;
            Object roomIdObj2 = game.get("room_id");
            Object endTimeObj = game.get("end_time");
            if (roomIdObj2 != null && roomIdObj2.toString().equals(roomId.toString()) && (endTimeObj == null || endTimeObj.toString().isEmpty())) {
                foundGameId = gid;
                record = game;
                break;
            }
        }
        if (record == null) {
            sendError(session, "未找到当前房间的进行中对局，无法悔棋");
            return;
        }
        // 2. 只允许对局双方悔棋
        Long blackId = record.get("black_id") == null ? null : Long.valueOf(record.get("black_id").toString());
        Long whiteId = record.get("white_id") == null ? null : Long.valueOf(record.get("white_id").toString());
        if (!userId.equals(blackId) && !userId.equals(whiteId)) {
            sendError(session, "您不是本局玩家，无法操作棋盘");
            return;
        }
        // 3. 撤销最后一步落子
        List<Object> allMoves = redisRoomManager.getGameMoves(foundGameId.toString());
        if (allMoves == null || allMoves.isEmpty()) {
            sendError(session, "当前无可撤销的落子");
            return;
        }
        redisRoomManager.getRedisTemplate().opsForList().rightPop("game:" + foundGameId + ":moves");
        // 4. 重建棋盘并广播
        List<Object> movesAfter = redisRoomManager.getGameMoves(foundGameId.toString());
        int[][] board = buildBoardFromMoves(movesAfter);
        JSONObject resp = new JSONObject();
        resp.put("board", board);
        playerSessionManager.broadcastToRoom(roomId, WSResult.undo(resp));
        watchSessionManager.broadcastToRoom(roomId, WSResult.undo(resp));
    }

    private void sendError(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.gameStateError(msg))));
        } catch (IOException ignored) {}
    }

    private int[][] buildBoardFromMoves(List<Object> moves) {
        int[][] board = new int[15][15];
        if (moves == null) return board;
        for (Object m : moves) {
            if (m instanceof Map) {
                Map mm = (Map) m;
                Object x = mm.get("x");
                Object y = mm.get("y");
                Object player = mm.get("player");
                if (x != null && y != null && player != null) {
                    board[Integer.parseInt(x.toString())][Integer.parseInt(y.toString())] = Integer.parseInt(player.toString());
                }
            }
        }
        return board;
    }
} 