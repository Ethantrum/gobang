package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.constant.RoomUserRoleConstant;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.GameMoves;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 恢复对局请求处理器：基于Redis分离结构实现。
 */
@Component
public class RestoreRequestHandler implements WebSocketMessageHandler {
    @Autowired
    private RedisRoomManager redisRoomManager;

    @WSMessageHandler("restore_request")
    public void handleRestoreRequest(WebSocketSession session, JSONObject data) {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) return;

        // 1. 查找未结束的GameRecord（room_id匹配且end_time为空，start_time最大）
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
        if (record == null) return;
        // 2. 查询所有落子
        List<Object> allMoves = redisRoomManager.getGameMoves(foundGameId.toString());
        List<GameMoves> movesList = new ArrayList<>();
        for (Object m : allMoves) {
            if (m instanceof Map) {
                Map mm = (Map) m;
                GameMoves gm = GameMoves.builder()
                        .gameId(foundGameId)
                        .moveIndex(mm.get("move_index") == null ? 0 : Integer.parseInt(mm.get("move_index").toString()))
                        .x(mm.get("x") == null ? 0 : Integer.parseInt(mm.get("x").toString()))
                        .y(mm.get("y") == null ? 0 : Integer.parseInt(mm.get("y").toString()))
                        .player(mm.get("player") == null ? 0 : Integer.parseInt(mm.get("player").toString()))
                        .build();
                movesList.add(gm);
            }
        }
        int[][] board = buildBoardFromMoves(movesList);
        // 3. 查询房间所有玩家和观战者
        Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomId.toString());
        Set<Object> watcherIds = redisRoomManager.getRoomWatcherIds(roomId.toString());
        // 4. 构造players信息，补充nickname和角色
        List<JSONObject> playerList = new ArrayList<>();
        if (playerIds != null) {
            for (Object uid : playerIds) {
                JSONObject ju = new JSONObject();
                ju.put("userId", uid);
                Map<Object, Object> userInfo = redisRoomManager.getRedisTemplate().opsForHash().entries("user:" + uid);
                ju.put("nickname", userInfo.getOrDefault("nickname", ""));
                Object blackId = record.get("black_id");
                Object whiteId = record.get("white_id");
                ju.put("isBlack", blackId != null && uid.toString().equals(blackId.toString()));
                ju.put("isWhite", whiteId != null && uid.toString().equals(whiteId.toString()));
                ju.put("isWatcher", false);
                playerList.add(ju);
            }
        }
        if (watcherIds != null) {
            for (Object uid : watcherIds) {
                JSONObject ju = new JSONObject();
                ju.put("userId", uid);
                Map<Object, Object> userInfo = redisRoomManager.getRedisTemplate().opsForHash().entries("user:" + uid);
                ju.put("nickname", userInfo.getOrDefault("nickname", ""));
                ju.put("isBlack", false);
                ju.put("isWhite", false);
                ju.put("isWatcher", true);
                playerList.add(ju);
            }
        }
        // 5. 计算当前回合
        int nextPlayer = 1;
        if (movesList != null && !movesList.isEmpty()) {
            nextPlayer = movesList.get(movesList.size() - 1).getPlayer() == 1 ? 2 : 1;
        }
        // 6. 组装恢复数据
        JSONObject resp = new JSONObject();
        resp.put("board", board);
        resp.put("nextPlayer", nextPlayer);
        resp.put("players", playerList);
        resp.put("winner", record.get("winner"));
        try {
            String restoreMsg = com.alibaba.fastjson.JSON.toJSONString(WSResult.restore(resp));
            session.sendMessage(new TextMessage(restoreMsg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int[][] buildBoardFromMoves(List<GameMoves> moves) {
        int[][] board = new int[15][15];
        for (GameMoves move : moves) {
            board[move.getX()][move.getY()] = move.getPlayer();
        }
        return board;
    }
} 