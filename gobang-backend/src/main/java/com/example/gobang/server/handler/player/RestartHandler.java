package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.mapper.GameRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RestartHandler implements WebSocketMessageHandler {
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private GameRecordMapper gameRecordMapper;

    @WSMessageHandler("restart")
    public void handleRestart(WebSocketSession session, JSONObject data) {
        try {
            Long userId = (Long) session.getAttributes().get("userId");
            Long roomId = (Long) session.getAttributes().get("roomId");
            if (userId == null || roomId == null) {
                playerSessionManager.sendToUser(roomId, userId, WSResult.error("用户或房间信息缺失"));
                return;
            }
            // 查询上局GameRecord
            GameRecord lastRecord = gameRecordMapper.selectOne(
                new QueryWrapper<GameRecord>().eq("room_id", roomId).orderByDesc("start_time").last("limit 1")
            );
            if (lastRecord == null) {
                playerSessionManager.sendToUser(roomId, userId, WSResult.error("未找到上一局对局，无法重开"));
                return;
            }
            Long lastBlackId = lastRecord.getBlackId();
            Long lastWhiteId = lastRecord.getWhiteId();
            if (lastBlackId == null || lastWhiteId == null) {
                playerSessionManager.sendToUser(roomId, userId, WSResult.error("上一局玩家信息异常"));
                return;
            }
            Long newBlackId = lastWhiteId;
            Long newWhiteId = lastBlackId;
            // 查询房间所有player
            List<RoomUser> playerUsers = roomUserMapper.selectList(
                new QueryWrapper<RoomUser>()
                    .eq("room_id", roomId)
                    .in("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER,
                                 com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_BLACK,
                                 com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WHITE)
            );
            if (playerUsers.size() < 2) {
                playerSessionManager.sendToUser(roomId, userId, WSResult.error("房间内玩家不足，无法开始新对局"));
                return;
            }
            // 先全部重置为普通player
            for (RoomUser ru : playerUsers) {
                if (ru.getRole() == com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_BLACK ||
                    ru.getRole() == com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WHITE ||
                    ru.getRole() == com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER) {
                    roomUserMapper.update(null, new UpdateWrapper<RoomUser>()
                        .eq("room_id", roomId).eq("user_id", ru.getUserId())
                        .set("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER));
                }
            }
            // 分配新黑白
            roomUserMapper.update(null, new UpdateWrapper<RoomUser>()
                .eq("room_id", roomId).eq("user_id", newBlackId)
                .set("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_BLACK));
            roomUserMapper.update(null, new UpdateWrapper<RoomUser>()
                .eq("room_id", roomId).eq("user_id", newWhiteId)
                .set("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WHITE));
            // 创建新对局
            GameRecord record = GameRecord.builder()
                    .roomId(roomId)
                    .blackId(newBlackId)
                    .whiteId(newWhiteId)
                    .startTime(java.time.LocalDateTime.now())
                    .build();
            gameRecordMapper.insert(record);
            List<User> users = userMapper.selectUsersByRoomId(roomId);
            JSONObject startData = new JSONObject();
            startData.put("players", users.stream().map(u -> {
                JSONObject ju = new JSONObject();
                ju.put("userId", u.getUserId());
                ju.put("nickname", u.getNickname());
                ju.put("isBlack", u.getUserId().equals(newBlackId));
                ju.put("isWhite", u.getUserId().equals(newWhiteId));
                return ju;
            }).collect(Collectors.toList()));
            startData.put("blackId", newBlackId);
            startData.put("whiteId", newWhiteId);
            startData.put("gameId", record.getId());
            playerSessionManager.broadcastToRoom(roomId, WSResult.start(startData));
        } catch (Exception e) {
            e.printStackTrace();
            Long roomId = null;
            Long userId = null;
            try {
                roomId = (Long) session.getAttributes().get("roomId");
                userId = (Long) session.getAttributes().get("userId");
            } catch (Exception ignore) {}
            playerSessionManager.broadcastToRoom(roomId, WSResult.error("再来一局失败：" + e.getMessage()));
        }
    }
} 