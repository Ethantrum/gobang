package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import com.example.gobang.server.mapper.GameRecordMapper;
import com.example.gobang.server.mapper.GameMovesMapper;
import com.example.gobang.server.mapper.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import java.io.IOException;
import java.util.List;
import java.awt.Point;
import java.util.ArrayList;

@Component
public class MoveHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private GameRecordMapper gameRecordMapper;
    @Autowired
    private GameMovesMapper gameMovesMapper;
    @Autowired
    private WatchSessionManager watchSessionManager;

    @WSMessageHandler("move")
    public void handleMove(WebSocketSession session, JSONObject data) throws IOException {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            return;
        }
        Integer x = data.getInteger("x");
        Integer y = data.getInteger("y");
        GameRecord record = gameRecordMapper.selectOne(
            new QueryWrapper<GameRecord>().eq("room_id", roomId).isNull("end_time")
        );
        if (record == null) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("对局不存在或已结束"))));
            return;
        }
        if (gameMovesMapper.selectCount(new QueryWrapper<GameMoves>().eq("game_id", record.getId()).eq("x", x).eq("y", y)) > 0) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("此处已有棋子"))));
            return;
        }
        long moveCount = gameMovesMapper.selectCount(new QueryWrapper<GameMoves>().eq("game_id", record.getId()));
        final Long blackId = record.getBlackId();
        final Long whiteId = record.getWhiteId();
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
        GameMoves move = GameMoves.builder()
                .gameId(record.getId())
                .moveIndex((int)moveCount + 1)
                .x(x).y(y).player(player).build();
        gameMovesMapper.insert(move);
        List<GameMoves> allMoves = gameMovesMapper.selectList(new QueryWrapper<GameMoves>().eq("game_id", record.getId()));
        int[][] board = buildBoardFromMoves(allMoves);
        WinResult winResult = checkWin(board, x, y, player);
        if (winResult.isWin()) {
            record.setEndTime(java.time.LocalDateTime.now());
            record.setWinner(userId);
            gameRecordMapper.updateById(record);
            Room room = roomMapper.selectById(roomId);
            if (room != null) {
                room.setStatus(com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_END);
                roomMapper.updateById(room);
            }
            JSONObject resultData = new JSONObject();
            resultData.put("winner", userId);
            resultData.put("board", board);
            resultData.put("winningLine", winResult.getWinningLine());
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

    private int[][] buildBoardFromMoves(List<GameMoves> moves) {
        int[][] board = new int[15][15];
        for (GameMoves move : moves) {
            board[move.getX()][move.getY()] = move.getPlayer();
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
} 