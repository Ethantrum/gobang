package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gobang.common.constant.RoomUserRoleConstant;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.mapper.GameMovesMapper;
import com.example.gobang.server.mapper.GameRecordMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RestoreRequestHandler implements WebSocketMessageHandler {
    @Autowired
    private GameRecordMapper gameRecordMapper;
    @Autowired
    private GameMovesMapper gameMovesMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private UserMapper userMapper;

    @WSMessageHandler("restore_request")
    public void handleRestoreRequest(WebSocketSession session, JSONObject data) {
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) return;

        // 查询当前未结束的GameRecord
        GameRecord record = gameRecordMapper.selectOne(
                new QueryWrapper<GameRecord>()
                        .eq("room_id", roomId)
                        .isNull("end_time")
                        .orderByDesc("start_time")
                        .last("limit 1")
        );
        if (record == null) return;

        // 查询所有落子
        List<GameMoves> allMoves = gameMovesMapper.selectList(
                new QueryWrapper<GameMoves>().eq("game_id", record.getId())
        );
        int[][] board = buildBoardFromMoves(allMoves);

        // 查询房间所有成员
        List<RoomUser> allRoomUsers = roomUserMapper.selectList(
                new QueryWrapper<RoomUser>().eq("room_id", roomId)
        );
        Map<Long, Byte> userRoleMap = allRoomUsers.stream().collect(Collectors.toMap(RoomUser::getUserId, RoomUser::getRole));
        // 查询所有用户信息
        List<Long> allUserIds = allRoomUsers.stream().map(RoomUser::getUserId).collect(Collectors.toList());
        List<User> users = allUserIds.isEmpty() ? List.of() : userMapper.selectBatchIds(allUserIds);

        // 组装players信息
        List<JSONObject> playerList = users.stream().map(u -> {
            JSONObject ju = new JSONObject();
            ju.put("userId", u.getUserId());
            ju.put("nickname", u.getNickname());
            ju.put("isBlack", u.getUserId().equals(record.getBlackId()));
            ju.put("isWhite", u.getUserId().equals(record.getWhiteId()));
            Byte role = userRoleMap.get(u.getUserId());
            ju.put("isWatcher", role != null && role.equals(RoomUserRoleConstant.ROLE_WATCH));
            return ju;
        }).collect(Collectors.toList());

        // 计算当前回合
        int nextPlayer = 1;
        if (allMoves != null && !allMoves.isEmpty()) {
            nextPlayer = allMoves.get(allMoves.size() - 1).getPlayer() == 1 ? 2 : 1;
        }

        // 组装恢复数据
        JSONObject resp = new JSONObject();
        resp.put("board", board);
        resp.put("nextPlayer", nextPlayer);
        resp.put("players", playerList);
        resp.put("winner", record.getWinner());
        // 可选：winningLine等

        try {
            String restoreMsg = com.alibaba.fastjson.JSON.toJSONString(WSResult.restore(resp));
            System.out.println("[WS][restore] send to userId=" + userId + ", roomId=" + roomId + ", msg=" + restoreMsg);
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