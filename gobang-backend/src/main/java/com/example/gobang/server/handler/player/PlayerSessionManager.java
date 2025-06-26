package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.pojo.entity.Room;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_FULL;
import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.gobang.pojo.entity.GameRecord;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.server.mapper.GameRecordMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.alibaba.fastjson.JSONObject;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_BLACK;
import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WHITE;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_READY;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.server.handler.watch.WatchSessionManager;

@Component
public class PlayerSessionManager {
    // roomId -> (userId -> Set<session>)
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Set<WebSocketSession>>> roomUserSessionsMap = new ConcurrentHashMap<>();

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private GameRecordMapper gameRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoomUserMapper roomUserMapper;

    @Autowired
    private WatchSessionManager watchSessionManager;

    /**
     * 注册玩家session，只允许一个session，多余的直接拒绝
     */
    public synchronized void registerPlayerSession(Long roomId, Long userId, WebSocketSession newSession) {
        roomUserSessionsMap.putIfAbsent(roomId, new ConcurrentHashMap<>());
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        userSessions.putIfAbsent(userId, new HashSet<>());
        Set<WebSocketSession> sessionSet = userSessions.get(userId);
        if (!sessionSet.isEmpty()) {
            try {
                newSession.close();
            } catch (IOException ignored) {}
            return;
        }
        sessionSet.add(newSession);
        // 检查房间内player数量，满2人则分配棋子并开始游戏
        if (getPlayerCount(roomId) == 2) {
            startGameForTwoPlayers(roomId, userSessions);
        }
    }

    /**
     * 移除一个玩家会话
     */
    public synchronized void removePlayerSession(WebSocketSession session) {
        Object roomIdObj = session.getAttributes().get("roomId");
        Object userIdObj = session.getAttributes().get("userId");
        if (!(roomIdObj instanceof Long) || !(userIdObj instanceof Long)) {
            return;
        }
        Long roomId = (Long) roomIdObj;
        Long userId = (Long) userIdObj;
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        if (userSessions == null) {
            return;
        }
        Set<WebSocketSession> sessionSet = userSessions.get(userId);
        if (sessionSet == null) {
            return;
        }
        sessionSet.remove(session);
        if (sessionSet.isEmpty()) {
            userSessions.remove(userId);
        }
        if (userSessions.isEmpty()) {
            roomUserSessionsMap.remove(roomId);
        }
        // 房主转让逻辑
        Room room = roomMapper.selectById(roomId);
        if (room != null && room.getOwnerId().equals(userId)) {
            // 查找剩余成员，优先player
            List<RoomUser> leftUsers = roomUserMapper.selectList(
                new QueryWrapper<RoomUser>()
                    .eq("room_id", roomId)
                    .ne("user_id", userId)
                    .orderByAsc("join_time")
            );
            RoomUser newOwner = null;
            // 先找player（包括黑白和普通player）
            for (RoomUser ru : leftUsers) {
                if (isPlayer(ru.getRole())) {
                    newOwner = ru;
                    break;
                }
            }
            // 没有player再找观战者
            if (newOwner == null) {
                for (RoomUser ru : leftUsers) {
                    if (!isPlayer(ru.getRole())) {
                        newOwner = ru;
                        break;
                    }
                }
            }
            if (newOwner != null) {
                room.setOwnerId(newOwner.getUserId());
                roomMapper.updateById(room);
                // 新增：如果新房主是观战者，确保其role为ROLE_WATCH，防止被统计为player
                if (!isPlayer(newOwner.getRole())) {
                    roomUserMapper.update(null, new UpdateWrapper<RoomUser>()
                        .eq("room_id", roomId)
                        .eq("user_id", newOwner.getUserId())
                        .set("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WATCH));
                }
                // 新增：房主转让后如果player数量不足2，房间状态设为等待
                List<Long> playerIdsAfter = getPlayerIds(roomId);
                if (playerIdsAfter.size() < 2) {
                    room.setStatus(ROOM_STATUS_READY);
                    roomMapper.updateById(room);
                }
            }
        }
        // 判定胜负与房间状态（只统计player）
        List<Long> playerIds = getPlayerIds(roomId);
        if (playerIds.size() == 1) {
            // 查询未结束的对局
            GameRecord record = gameRecordMapper.selectOne(
                new QueryWrapper<GameRecord>().eq("room_id", roomId).isNull("end_time")
            );
            if (record != null) {
                Long winnerId = playerIds.get(0);
                record.setEndTime(java.time.LocalDateTime.now());
                record.setWinner(winnerId);
                gameRecordMapper.updateById(record);
                com.alibaba.fastjson.JSONObject resultData = new com.alibaba.fastjson.JSONObject();
                resultData.put("winner", winnerId);
                broadcastToRoom(roomId, com.example.gobang.common.result.WSResult.result(resultData));
            }
            // 修改房间状态为等待
            if (room != null) {
                room.setStatus(ROOM_STATUS_READY);
                roomMapper.updateById(room);
            }
        }
    }

    /**
     * 向指定房间的所有玩家会话广播消息
     */
    public synchronized void broadcastToRoom(Long roomId, Object message) {
        String messageJson = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(messageJson);
        roomUserSessionsMap.get(roomId).values().stream()
                .flatMap(Set::stream)
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException ignored) {}
                });
        // 回滚：不再强制推送给观战者，防止空指针
        // 如需观战推送，请在业务层显式判断观战session是否存在再推送
    }

    /**
     * 向指定玩家发送消息
     */
    public synchronized void sendToUser(Long roomId, Long userId, Object message) {
        String messageJson = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(messageJson);
        roomUserSessionsMap.get(roomId).get(userId).stream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException ignored) {}
                });
    }

    // 获取房间内player数量
    private int getPlayerCount(Long roomId) {
        ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = roomUserSessionsMap.get(roomId);
        if (userSessions == null) return 0;
        // 这里只统计player角色的数量
        // 实际上你可以根据业务调整统计逻辑
        return userSessions.size();
    }

    // 封装分配棋子和开始游戏的私有方法
    private void startGameForTwoPlayers(Long roomId, ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions) {
        // 1. 设置房间状态为满员
        Room room = roomMapper.selectById(roomId);
        if (room != null) {
            room.setStatus(ROOM_STATUS_FULL);
            roomMapper.updateById(room);
        }
        // 2. 分配黑白棋子
        List<Long> playerIds = new ArrayList<>(userSessions.keySet());
        Long blackId = playerIds.get(0);
        Long whiteId = playerIds.get(1);
        // 2.1 更新room_user表角色
        updatePlayerRole(roomId, blackId, ROLE_BLACK);
        updatePlayerRole(roomId, whiteId, ROLE_WHITE);
        // 3. 创建对局记录
        GameRecord record = GameRecord.builder()
                .roomId(roomId)
                .blackId(blackId)
                .whiteId(whiteId)
                .startTime(java.time.LocalDateTime.now())
                .build();
        gameRecordMapper.insert(record);
        // 4. 查询玩家信息
        List<User> users = userMapper.selectBatchIds(playerIds);
        // 5. 构造并广播开始消息
        JSONObject startData = new JSONObject();
        startData.put("players", users.stream().map(u -> {
            JSONObject ju = new JSONObject();
            ju.put("userId", u.getUserId());
            ju.put("nickname", u.getNickname());
            ju.put("isBlack", u.getUserId().equals(blackId));
            ju.put("isWhite", u.getUserId().equals(whiteId));
            return ju;
        }).collect(java.util.stream.Collectors.toList()));
        startData.put("blackId", blackId);
        startData.put("whiteId", whiteId);
        startData.put("gameId", record.getId());
        broadcastToRoom(roomId, WSResult.start(startData));
    }

    // 封装：更新room_user表角色
    private void updatePlayerRole(Long roomId, Long userId, byte role) {
        roomUserMapper.update(
            null,
            new UpdateWrapper<com.example.gobang.pojo.entity.RoomUser>()
                .eq("room_id", roomId)
                .eq("user_id", userId)
                .set("role", role)
        );
    }

    // 判断是否为player（包括普通player、黑棋、白棋）
    private boolean isPlayer(byte role) {
        return role == ROLE_PLAYER || role == ROLE_BLACK || role == ROLE_WHITE;
    }

    // 获取房间内player角色的用户ID
    public List<Long> getPlayerIds(Long roomId) {
        List<RoomUser> users = roomUserMapper.selectList(
            new QueryWrapper<RoomUser>().eq("room_id", roomId)
        );
        List<Long> playerIds = new ArrayList<>();
        for (RoomUser ru : users) {
            if (isPlayer(ru.getRole())) {
                playerIds.add(ru.getUserId());
            }
        }
        return playerIds;
    }
} 