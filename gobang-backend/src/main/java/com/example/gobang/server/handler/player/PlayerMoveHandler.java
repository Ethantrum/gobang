package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import com.example.gobang.server.service.GameArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import java.io.IOException;
import java.util.List;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

@Component
public class PlayerMoveHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RedisRoomManager redisRoomManager;
    @Autowired
    private GameArchiveService gameArchiveService;

    @WSMessageHandler("move")
    public void handleMove(WebSocketSession session, JSONObject data) throws IOException {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            return;
        }
        Integer x = data.getInteger("x");
        Integer y = data.getInteger("y");
        // --- Redis实现 ---
        // 1. 查找未结束的对局
        Long foundGameId = null;
        Map<Object, Object> record = null;
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
            if (roomIdObj2 != null && roomIdObj2.toString().equals(roomId.toString()) && (endTimeObj == null || endTimeObj.toString().isEmpty())) {
                foundGameId = gid;
                record = game;
                break;
            }
        }
        if (record == null) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.kick("对局不存在或已结束"))));
            return;
        }
        // 2. 检查该位置是否已有棋子
        List<Object> allMoves = redisRoomManager.getGameMoves(foundGameId.toString());
        for (Object moveObj : allMoves) {
            if (moveObj instanceof Map) {
                Map move = (Map) moveObj;
                Object mx = move.get("x");
                Object my = move.get("y");
                if (mx != null && my != null && mx.toString().equals(x.toString()) && my.toString().equals(y.toString())) {
                    session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.validationError("此处已有棋子"))));
                    return;
                }
            }
        }
        long moveCount = allMoves.size();
        final Long blackId = record.get("black_id") == null ? null : Long.valueOf(record.get("black_id").toString());
        final Long whiteId = record.get("white_id") == null ? null : Long.valueOf(record.get("white_id").toString());
        boolean isBlackTurn = moveCount % 2 == 0;
        Integer player;
        if (isBlackTurn && userId.equals(blackId)) {
            player = 1;
        } else if (!isBlackTurn && userId.equals(whiteId)) {
            player = 2;
        } else {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("不是您的回合"))));
            return;
        }
        // 3. 落子
        Map<String, Object> move = new java.util.HashMap<>();
        move.put("game_id", foundGameId);
        move.put("move_index", (int)moveCount + 1);
        move.put("x", x);
        move.put("y", y);
        move.put("player", player);
        move.put("move_time", System.currentTimeMillis());
        redisRoomManager.addGameMove(foundGameId.toString(), move);
        // 4. 构建棋盘
        List<Object> allMovesAfter = redisRoomManager.getGameMoves(foundGameId.toString());
        int[][] board = buildBoardFromMoves(allMovesAfter);
        WinResult winResult = checkWin(board, x, y, player);
        if (winResult.isWin()) {
            record.put("end_time", String.valueOf(System.currentTimeMillis()));
            record.put("winner", userId);
            redisRoomManager.createGameRecord(foundGameId.toString(), convertToStringObjectMap(record));
            Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomId.toString());
            if (roomInfo != null) {
                roomInfo.put("status", com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_END);
                redisRoomManager.createRoom(roomId.toString(), convertToStringObjectMap(roomInfo));
            }
            
            // TODO: 游戏结束归档 - 调用GameArchiveService.archiveGame(foundGameId)将游戏数据归档到MySQL
            gameArchiveService.archiveGame(roomId, foundGameId);
            
            JSONObject resultData = new JSONObject();
            resultData.put("winner", userId);
            resultData.put("board", board);
            List<Point> rawLine = winResult.getWinningLine();
            List<List<Integer>> line = new ArrayList<>();
            if (rawLine != null) {
                for (Point p : rawLine) {
                    line.add(Arrays.asList(p.x, p.y));
                }
            }
            resultData.put("winningLine", line);
            playerSessionManager.broadcastToRoom(roomId, WSResult.result(resultData));
            watchSessionManager.broadcastToRoom(roomId, WSResult.result(resultData));
            return;
        }
        JSONObject resp = new JSONObject();
        resp.put("board", board);
        resp.put("nextPlayer", player == 1 ? 2 : 1);
        playerSessionManager.broadcastToRoom(roomId, WSResult.move(resp));
        watchSessionManager.broadcastToRoom(roomId, WSResult.move(resp));
    }

    private WinResult checkWin(int[][] board, int x, int y, int player) {
        final int[] dx = {1, 0, 1, 1};
        final int[] dy = {0, 1, 1, -1};
        for (int i = 0; i < 4; i++) {
            List<Point> line = new ArrayList<>();
            line.add(new Point(x, y));
            int count = 1;
            for (int j = 1; j < 5; j++) {
                int nx = x + j * dx[i];
                int ny = y + j * dy[i];
                if (nx >= 0 && nx < 15 && ny >= 0 && ny < 15 && board[nx][ny] == player) {
                    count++;
                    line.add(new Point(nx, ny));
                } else {
                    break;
                }
            }
            for (int j = 1; j < 5; j++) {
                int nx = x - j * dx[i];
                int ny = y - j * dy[i];
                if (nx >= 0 && nx < 15 && ny >= 0 && ny < 15 && board[nx][ny] == player) {
                    count++;
                    line.add(new Point(nx, ny));
                } else {
                    break;
                }
            }
            if (count >= 5) {
                return new WinResult(true, line);
            }
        }
        return new WinResult(false, null);
    }

    private int[][] buildBoardFromMoves(List<Object> moves) {
        int[][] board = new int[15][15];
        for (Object moveObj : moves) {
            if (moveObj instanceof Map) {
                Map move = (Map) moveObj;
                Object x = move.get("x");
                Object y = move.get("y");
                Object player = move.get("player");
                if (x != null && y != null && player != null) {
                    board[Integer.parseInt(x.toString())][Integer.parseInt(y.toString())] = Integer.parseInt(player.toString());
                }
            }
        }
        return board;
    }

    private static class WinResult {
        private final boolean isWin;
        private final List<Point> winningLine;
        public WinResult(boolean isWin, List<Point> winningLine) {
            this.isWin = isWin;
            this.winningLine = winningLine;
        }
        public boolean isWin() { return isWin; }
        public List<Point> getWinningLine() { return winningLine; }
    }

    private static Map<String, Object> convertToStringObjectMap(Map<Object, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }
} 