package com.example.gobang.server.handler.watch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.mapper.GameRecordMapper;
import com.example.gobang.server.mapper.GameMovesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.List;

@Component
public class WatchJoinHandler implements WebSocketMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(WatchJoinHandler.class);
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private GameRecordMapper gameRecordMapper;
    @Autowired
    private GameMovesMapper gameMovesMapper;

    @WSMessageHandler("watchJoin")
    public void handleWatchJoin(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("[观战] 收到watchJoin, roomId={}, userId={}, data={}", roomId, userId, data);
        if (roomId == null || userId == null) {
            return;
        }
        // 只注册观战session，不动数据库role
        watchSessionManager.registerWatchSession(roomId, userId, session);

        // 1. 推送观战身份
        JSONObject watchMsg = new JSONObject();
        watchMsg.put("isWatcher", true);
        JSONObject msg = new JSONObject();
        msg.put("type", "watch");
        msg.put("data", watchMsg);
        log.info("[观战] 推送身份消息: {}", msg);
        watchSessionManager.sendToUser(roomId, userId, msg);
        // 2. 推送棋盘最新状态（用move类型）
        GameRecord record = gameRecordMapper.selectOne(
            new QueryWrapper<GameRecord>().eq("room_id", roomId).isNull("end_time").orderByDesc("start_time").last("limit 1")
        );
        int[][] board = new int[15][15];
        int nextPlayer = 1;
        if (record != null) {
            List<GameMoves> moves = gameMovesMapper.selectList(
                new QueryWrapper<GameMoves>().eq("game_id", record.getId()).orderByAsc("move_index")
            );
            for (GameMoves move : moves) {
                board[move.getX()][move.getY()] = move.getPlayer();
            }
            nextPlayer = (moves.size() % 2 == 0) ? 1 : 2;
        }
        JSONObject moveMsg = new JSONObject();
        moveMsg.put("board", board);
        moveMsg.put("nextPlayer", nextPlayer);
        log.info("[观战] 推送棋盘消息: {}", moveMsg);
        watchSessionManager.sendToUser(roomId, userId, com.example.gobang.common.result.WSResult.move(moveMsg));
    }
} 