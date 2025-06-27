package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.mapper.GameRecordMapper;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RestartResponseHandler implements WebSocketMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(RestartResponseHandler.class);
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
    @Autowired
    private WatchSessionManager watchSessionManager;

    @WSMessageHandler("restart_response")
    public void handleRestartResponse(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long fromUserId = data.getLong("fromUserId");
        if (roomId == null || fromUserId == null) {
            return;
        }
        boolean agree = data.getBoolean("agree");
        playerSessionManager.sendToUser(roomId, fromUserId, WSResult.restartResponse(agree));
        if (agree) {
            try {
                Long userId = (Long) session.getAttributes().get("userId");
                log.info("[再来一局] restart_response同意, roomId={}, userId={}, fromUserId={}", roomId, userId, fromUserId);
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
                // 查询房间所有成员（包括观战者）
                List<RoomUser> allRoomUsers = roomUserMapper.selectList(
                    new QueryWrapper<RoomUser>().eq("room_id", roomId)
                );
                // 查询所有用户信息
                List<Long> allUserIds = allRoomUsers.stream().map(RoomUser::getUserId).collect(Collectors.toList());
                List<User> users = allUserIds.isEmpty() ? List.of() : userMapper.selectBatchIds(allUserIds);
                // 构建 userId -> role 映射
                java.util.Map<Long, Byte> userRoleMap = allRoomUsers.stream().collect(Collectors.toMap(RoomUser::getUserId, RoomUser::getRole));
                // 先全部重置为普通player
                List<RoomUser> playerUsers = allRoomUsers.stream()
                    .filter(ru -> ru.getRole() == com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_BLACK
                               || ru.getRole() == com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WHITE
                               || ru.getRole() == com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER)
                    .collect(Collectors.toList());
                if (playerUsers.size() < 2) {
                    playerSessionManager.sendToUser(roomId, userId, WSResult.error("房间内玩家不足，无法开始新对局"));
                    return;
                }
                for (RoomUser ru : playerUsers) {
                    roomUserMapper.update(null, new UpdateWrapper<RoomUser>()
                        .eq("room_id", roomId).eq("user_id", ru.getUserId())
                        .set("role", com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER));
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
                JSONObject startData = new JSONObject();
                startData.put("players", users.stream().map(u -> {
                    JSONObject ju = new JSONObject();
                    ju.put("userId", u.getUserId());
                    ju.put("nickname", u.getNickname());
                    ju.put("isBlack", u.getUserId().equals(newBlackId));
                    ju.put("isWhite", u.getUserId().equals(newWhiteId));
                    Byte role = userRoleMap.get(u.getUserId());
                    ju.put("isWatcher", role != null && role.equals(com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WATCH));
                    return ju;
                }).collect(Collectors.toList()));
                startData.put("blackId", newBlackId);
                startData.put("whiteId", newWhiteId);
                startData.put("gameId", record.getId());
                log.info("[再来一局] 直接推送start消息: {}", startData);
                playerSessionManager.broadcastToRoom(roomId, WSResult.start(startData));
                // 新增：推送给观战者
                watchSessionManager.broadcastToRoom(roomId, WSResult.start(startData));
            } catch (Exception e) {
                e.printStackTrace();
                playerSessionManager.broadcastToRoom(roomId, WSResult.error("再来一局失败：" + e.getMessage()));
            }
        }
    }
} 