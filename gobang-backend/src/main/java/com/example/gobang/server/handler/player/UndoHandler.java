package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.mapper.GameRecordMapper;
import com.example.gobang.server.mapper.GameMovesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import java.io.IOException;
import java.util.List;

@Component
public class UndoHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private GameRecordMapper gameRecordMapper;
    @Autowired
    private GameMovesMapper gameMovesMapper;

    @WSMessageHandler("undo")
    public void handleUndo(WebSocketSession session, JSONObject data) {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            return;
        }
        GameRecord record = gameRecordMapper.selectOne(
            new QueryWrapper<GameRecord>()
                .eq("room_id", roomId)
                .isNull("end_time")
                .orderByDesc("start_time")
                .last("limit 1")
        );
        if (record == null) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("未找到当前房间的进行中对局，无法悔棋"))));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        if (!userId.equals(record.getBlackId()) && !userId.equals(record.getWhiteId())) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("您不是本局玩家，无法操作棋盘"))));
            } catch (IOException ignored) {}
            return;
        }
        Long gameId = record.getId();
        List<GameMoves> moves = gameMovesMapper.selectList(
                new QueryWrapper<GameMoves>().eq("game_id", gameId)
                        .orderByDesc("id").last("limit 1"));
        if (!moves.isEmpty()) {
            gameMovesMapper.deleteById(moves.get(0).getId());
        }
        List<GameMoves> allMoves = gameMovesMapper.selectList(
                new QueryWrapper<GameMoves>().eq("game_id", gameId));
        int[][] board = buildBoardFromMoves(allMoves);
        JSONObject resp = new JSONObject();
        resp.put("board", board);
        playerSessionManager.broadcastToRoom(roomId, WSResult.undo(resp));
    }

    private int[][] buildBoardFromMoves(List<GameMoves> moves) {
        int[][] board = new int[15][15];
        for (GameMoves move : moves) {
            board[move.getX()][move.getY()] = move.getPlayer();
        }
        return board;
    }
} 