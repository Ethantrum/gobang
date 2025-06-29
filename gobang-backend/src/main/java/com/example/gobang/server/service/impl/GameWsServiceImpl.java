package com.example.gobang.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.handler.WSSessionManager;
import com.example.gobang.server.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.awt.Point;
import java.util.stream.Collectors;

/**
 * GameWsService 是 WebSocket 消息的业务处理类。
 * 实现 WebSocketMessageHandler 接口，便于自动注册。
 * 
 * 你只需要在这里写上带有 @WSMessageHandler("type") 注解的方法，
 * MyWebSocketHandler 会通过反射自动扫描这些方法，并根据前端发来的 type 字段自动分发调用。
 * 
 * 方法签名要求：
 *   - 第一个参数必须是 WebSocketSession，表示当前连接。
 *   - 第二个参数是 JSONObject，表示前端 data 字段内容（会自动转换）。
 *   - 返回值类型可以自定义，最终会作为 data 字段回写给前端。
 * 
 * 反射分发原理：
 *   - 启动时，WSDispatcher 会扫描本类所有方法，找到带 @WSMessageHandler 的方法，建立 type 到方法的映射。
 *   - 收到消息后，根据 type 找到对应方法，并用反射调用，自动注入参数。
 *   - 你只需专注写业务逻辑，不用写分发代码。
 */
@Component
public class GameWsServiceImpl implements WebSocketMessageHandler {


    @Autowired
    private WSSessionManager wsSessionManager;
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

    /**
     * 用户加入房间
     * 前端需传递 roomId, userId
     * 返回房间内所有玩家信息
     */
    @WSMessageHandler("join")
    public void handleJoin(WebSocketSession session, JSONObject data) {
        System.out.println("[GameWsServiceImpl] handleJoin 被调用, data=" + data);
        Long roomId = data.getLong("roomId");
        Long userId = data.getLong("userId");

        // 将用户信息存入 session
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("roomId", roomId);

        // 幂等插入
        if (roomUserMapper.selectCount(new QueryWrapper<RoomUser>()
                .eq("room_id", roomId).eq("user_id", userId)) == 0) {
            RoomUser ru = RoomUser.builder().roomId(roomId).userId(userId).build();
            roomUserMapper.insert(ru);
        }
        // 查询房间所有用户（User信息）
        List<User> users = userMapper.selectUsersByRoomId(roomId);

        // 检查房间人数，如果满2人则开始游戏
        if (users.size() == 2) {
            // 更新房间状态为游戏中
            Room room = roomMapper.selectById(roomId);
            if (room != null && room.getStatus() == 0) { // 只有等待中的房间才能开始
                room.setStatus((byte)1); // 1-游戏中
                roomMapper.updateById(room);

                // 创建新的对局记录
                GameRecord record = GameRecord.builder()
                        .roomId(roomId)
                        .blackId(users.get(0).getUserId()) // 默认先加入的为黑棋
                        .whiteId(users.get(1).getUserId())
                        .startTime(java.time.LocalDateTime.now())
                        .build();
                gameRecordMapper.insert(record);

                // 广播游戏开始消息
                Long blackId = record.getBlackId();
                Long whiteId = record.getWhiteId();
                JSONObject startData = new JSONObject();
                startData.put("players", users.stream().map(u -> {
                    JSONObject ju = new JSONObject();
                    ju.put("userId", u.getUserId());
                    ju.put("nickname", u.getNickname());
                    ju.put("isBlack", u.getUserId().equals(blackId));
                    return ju;
                }).collect(Collectors.toList()));
                startData.put("blackId", blackId);
                startData.put("whiteId", whiteId);
                startData.put("gameId", record.getId());
                wsSessionManager.broadcastToRoom(roomId, WSResult.start(startData));
                return; // 结束，不再发送普通的join消息
            }
        }
        // 如果人数不足2人或房间状态不正确，则只广播玩家列表
        JSONObject resp = new JSONObject();
        resp.put("players", users.stream().map(u -> {
            JSONObject ju = new JSONObject();
            ju.put("userId", u.getUserId());
            ju.put("nickname", u.getNickname());
            return ju;
        }).collect(Collectors.toList()));
        wsSessionManager.broadcastToRoom(roomId, WSResult.join(resp));
    }

    /**
     * 用户落子
     * 前端需传递 roomId, x, y, player
     * 返回最新棋盘和下一个玩家
     */
    @WSMessageHandler("move")
    public void handleMove(WebSocketSession session, JSONObject data) throws IOException {
        System.out.println("[GameWsServiceImpl] handleMove 被调用, data=" + data);
        Long userId = (Long) session.getAttributes().get("userId");
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (userId == null || roomId == null) {
            return; // 忽略无效请求
        }

        Integer x = data.getInteger("x");
        Integer y = data.getInteger("y");

        // 查找当前活跃对局
        GameRecord record = gameRecordMapper.selectOne(
            new QueryWrapper<GameRecord>().eq("room_id", roomId).isNull("end_time")
        );

        if (record == null) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("对局不存在或已结束"))));
            return;
        }

        // 检查是否已落子
        if (gameMovesMapper.selectCount(new QueryWrapper<GameMoves>().eq("game_id", record.getId()).eq("x", x).eq("y", y)) > 0) {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("此处已有棋子"))));
            return;
        }

        // 回合制判断
        long moveCount = gameMovesMapper.selectCount(new QueryWrapper<GameMoves>().eq("game_id", record.getId()));
        Long blackId = record.getBlackId();
        Long whiteId = record.getWhiteId();

        boolean isBlackTurn = moveCount % 2 == 0;
        Integer player; // 1-黑, 2-白

        if (isBlackTurn && userId.equals(blackId)) {
            player = 1;
        } else if (!isBlackTurn && userId.equals(whiteId)) {
            player = 2;
        } else {
            session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("不是您的回合"))));
            return;
        }
        
        // 插入落子
        GameMoves move = GameMoves.builder()
                .gameId(record.getId())
                .moveIndex((int)moveCount + 1)
                .x(x).y(y).player(player).build();
        gameMovesMapper.insert(move);

        // 获取完整棋盘
        List<GameMoves> allMoves = gameMovesMapper.selectList(new QueryWrapper<GameMoves>().eq("game_id", record.getId()));
        int[][] board = buildBoardFromMoves(allMoves);

        // 胜利判断
        WinResult winResult = checkWin(board, x, y, player);
        if (winResult.isWin()) {
            // 更新对局和房间状态
            record.setEndTime(java.time.LocalDateTime.now());
            record.setWinner(userId);
            gameRecordMapper.updateById(record);

            Room room = roomMapper.selectById(roomId);
            if (room != null) {
                // 游戏结束，房间状态回到等待中，以便可以再来一局或加入新玩家
                room.setStatus((byte)0); // 0-等待中
                roomMapper.updateById(room);
            }
            
            // 广播胜利结果，并附带高亮坐标
            JSONObject resultData = new JSONObject();
            resultData.put("winner", userId);
            resultData.put("board", board);
            resultData.put("winningLine", winResult.getWinningLine());
            wsSessionManager.broadcastToRoom(roomId, WSResult.result(resultData));
            return;
        }
        
        // 如果未分出胜负，则广播下一步
        JSONObject resp = new JSONObject();
        resp.put("board", board);
        resp.put("nextPlayer", player == 1 ? 2 : 1);
        wsSessionManager.broadcastToRoom(roomId, WSResult.move(resp));
    }

    /**
     * 用户悔棋
     * 前端需传递 roomId
     * 返回最新棋盘
     */
    @WSMessageHandler("undo")
    public void handleUndo(WebSocketSession session, JSONObject data) {
        System.out.println("[GameWsServiceImpl] handleUndo 被调用, data=" + data);
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (roomId == null) {
            return;
        }
        // 查找当前活跃对局
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
        Long gameId = record.getId();
        // 删除最后一步
        List<GameMoves> moves = gameMovesMapper.selectList(
                new QueryWrapper<GameMoves>().eq("game_id", gameId)
                        .orderByDesc("id").last("limit 1"));
        if (!moves.isEmpty()) {
            gameMovesMapper.deleteById(moves.get(0).getId());
        }
        // 查询最新棋盘
        List<GameMoves> allMoves = gameMovesMapper.selectList(
                new QueryWrapper<GameMoves>().eq("game_id", gameId));
        int[][] board = buildBoardFromMoves(allMoves);
        JSONObject resp = new JSONObject();
        resp.put("board", board);
        wsSessionManager.broadcastToRoom(roomId, WSResult.undo(resp));
    }

    /**
     * 重开对局
     * 前端需传递 roomId
     * 返回空棋盘
     */
    @WSMessageHandler("restart")
    public void handleRestart(WebSocketSession session, JSONObject data) {
        System.out.println("[GameWsServiceImpl] handleRestart 被调用, data=" + data);
        Long roomId = (Long) session.getAttributes().get("roomId");
        if (roomId == null) {
            return;
        }

        List<RoomUser> roomUsers = roomUserMapper.selectList(
                new QueryWrapper<RoomUser>().eq("room_id", roomId));
        if (roomUsers.size() < 2) {
            return; // 人数不足，无法重开
        }

        // 重置房间状态为游戏中
        Room room = roomMapper.selectById(roomId);
        if (room != null) {
            room.setStatus((byte)1); // 1-游戏中
            roomMapper.updateById(room);
        }

        // 为了公平，交换黑白方
        GameRecord record = GameRecord.builder()
                .roomId(roomId)
                .blackId(roomUsers.get(1).getUserId()) // 原来的第二个玩家执黑
                .whiteId(roomUsers.get(0).getUserId()) // 原来的第一个玩家执白
                .startTime(java.time.LocalDateTime.now())
                .build();
        gameRecordMapper.insert(record);

        // 获取 User 信息
        List<User> users = userMapper.selectUsersByRoomId(roomId);
        Long blackId = record.getBlackId();
        Long whiteId = record.getWhiteId();
        JSONObject startData = new JSONObject();
        startData.put("players", users.stream().map(u -> {
            JSONObject ju = new JSONObject();
            ju.put("userId", u.getUserId());
            ju.put("nickname", u.getNickname());
            ju.put("isBlack", u.getUserId().equals(blackId));
            return ju;
        }).collect(Collectors.toList()));
        startData.put("blackId", blackId);
        startData.put("whiteId", whiteId);
        startData.put("gameId", record.getId());
        wsSessionManager.broadcastToRoom(roomId, WSResult.start(startData));
    }

    /**
     * 离开房间
     * 无需前端传递参数，通过 session 获取
     * 返回成功消息
     */
    @WSMessageHandler("leave")
    public void handleLeave(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId == null || userId == null) return;

        User leavingUser = userMapper.selectById(userId);
        String nickname = (leavingUser != null) ? leavingUser.getNickname() : "一位玩家";

        // 从数据库中移除 room_user 记录
        roomUserMapper.delete(new QueryWrapper<RoomUser>().eq("room_id", roomId).eq("user_id", userId));

        // 检查房间是否还有人
        long memberCount = roomUserMapper.selectCount(new QueryWrapper<RoomUser>().eq("room_id", roomId));
        if (memberCount == 0) {
            // 没人了，删除房间
            roomMapper.deleteById(roomId);
        } else {
            Room room = roomMapper.selectById(roomId);
            // 查找当前活跃对局
            GameRecord record = gameRecordMapper.selectOne(
                    new QueryWrapper<GameRecord>().eq("room_id", roomId).isNull("end_time")
            );

            if (record != null) {
                // 如果对局正在进行中，则离开者判负
                record.setEndTime(java.time.LocalDateTime.now());
                Long winnerId = record.getBlackId().equals(userId) ? record.getWhiteId() : record.getBlackId();
                record.setWinner(winnerId);
                gameRecordMapper.updateById(record);

                // 更新房间状态为等待中
                if(room != null) {
                    room.setStatus((byte) 0); // 0-等待中
                    roomMapper.updateById(room);
                }

                // 广播游戏结果
                JSONObject resultData = new JSONObject();
                resultData.put("winner", winnerId);
                resultData.put("board", buildBoardFromMoves(gameMovesMapper.selectList(new QueryWrapper<GameMoves>().eq("game_id", record.getId()))));
                wsSessionManager.broadcastToRoom(roomId, WSResult.result(resultData));
            } else if (room != null && room.getOwnerId().equals(userId)) {
                // 如果不是在对局中离开，且离开的是房主，则转让房主
                List<RoomUser> remainingUsers = roomUserMapper.selectList(
                        new QueryWrapper<RoomUser>().eq("room_id", roomId).orderByAsc("join_time"));
                if (!remainingUsers.isEmpty()) {
                    room.setOwnerId(remainingUsers.get(0).getUserId());
                    roomMapper.updateById(room);
                }
            }

            // 广播离开消息
            JSONObject leaveData = new JSONObject();
            leaveData.put("userId", userId);
            leaveData.put("nickname", nickname);
            wsSessionManager.broadcastToRoom(roomId, WSResult.leave(leaveData));
        }

        // 清理 session，以防复用
        session.getAttributes().remove("roomId");
        session.getAttributes().remove("userId");
    }

    /**
     * 胜利逻辑判断
     */
    private WinResult checkWin(int[][] board, int x, int y, int player) {
        final int[] dx = {1, 0, 1, 1};
        final int[] dy = {0, 1, 1, -1};

        for (int i = 0; i < 4; i++) {
            List<Point> line = new ArrayList<>();
            line.add(new Point(x, y));
            int count = 1;

            // 正方向
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

            // 反方向
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

    /**
     * 辅助方法：根据落子记录生成棋盘
     */
    private int[][] buildBoardFromMoves(List<GameMoves> moves) {
        int[][] board = new int[15][15];
        for (GameMoves move : moves) {
            board[move.getX()][move.getY()] = move.getPlayer();
        }
        return board;
    }

    /**
     * 再来一局请求
     * 前端需传递 roomId
     * 服务端转发给对方
     */
    @WSMessageHandler("restartRequest")
    public void handleRestartRequest(WebSocketSession session, JSONObject data) {
        System.out.println("[GameWsServiceImpl] handleRestartRequest 被调用, data=" + data);
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId == null || userId == null) return;

        // 核心修复：检查房间人数是否为2
        long memberCount = roomUserMapper.selectCount(new QueryWrapper<RoomUser>().eq("room_id", roomId));
        if (memberCount < 2) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(WSResult.error("对方已离开，无法开始新对局。"))));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // 向对方发送请求
        List<User> users = userMapper.selectUsersByRoomId(roomId);
        for (User u : users) {
            if (!u.getUserId().equals(userId)) {
                wsSessionManager.sendToUser(u.getUserId(), WSResult.restart(JSON.parseObject("{" +
                        "\"fromUserId\":" + userId +
                        ",\"roomId\":" + roomId +
                        "}")));
            }
        }
    }

    /**
     * 再来一局响应
     * agree=true时才真正重开，否则通知发起方被拒绝
     */
    @WSMessageHandler("restartResponse")
    public void handleRestartResponse(WebSocketSession session, JSONObject data) {
        System.out.println("[GameWsServiceImpl] handleRestartResponse 被调用, data=" + data);
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        Boolean agree = data.getBoolean("agree");
        Long fromUserId = data.getLong("fromUserId"); // 发起方ID
        if (roomId == null || userId == null || fromUserId == null) return;
        if (agree != null && agree) {
            handleRestart(session, new JSONObject());
        } else {
            wsSessionManager.sendToUser(fromUserId, WSResult.error("对方拒绝再来一局，已自动离开房间"));
        }
    }

    /**
     * 新的内部类，用于返回胜利结果
     */
    private static class WinResult {
        private final boolean isWin;
        private final List<Point> winningLine;

        public WinResult(boolean isWin, List<Point> winningLine) {
            this.isWin = isWin;
            this.winningLine = winningLine;
        }

        public boolean isWin() {
            return isWin;
        }

        public List<Point> getWinningLine() {
            return winningLine;
        }
    }
} 