package com.example.gobang.server.service.room.impl;

import com.example.gobang.common.result.Result;
import com.example.gobang.server.service.room.RoomJoinService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.pojo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_END;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_FULL;
import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_MAX_PLAYER;

/**
 * 房间加入服务实现：处理用户加入房间的业务逻辑。
 * 只允许玩家身份加入，自动处理观战身份切换和反向索引。
 */
@Service
public class RoomJoinServiceImpl implements RoomJoinService {

    @Autowired
    private RedisRoomManager redisRoomManager;

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户加入房间。
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 加入结果，成功返回房间ID，失败返回错误信息
     */
    @Override
    public Result roomJoin(Long roomId, Long userId) {
        Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomId.toString());
        if (roomInfo == null || roomInfo.isEmpty()) {
            return Result.error("房间不存在");
        }
        // 加入房间前，先移除观战身份及其反向索引，防止同一用户重复身份
        redisRoomManager.removeRoomWatcher(roomId.toString(), userId.toString());
        redisRoomManager.getRedisTemplate().delete("user:" + userId + ":currentWatchRoom");
        // 判断用户是否已在玩家集合
        Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomId.toString());
        if (playerIds != null && playerIds.contains(userId.toString())) {
            return Result.error("用户已经在房间中");
        }
        // 新增：判断房间人数是否已满
        if (playerIds != null && playerIds.size() >= ROOM_MAX_PLAYER) {
            // 设置房间状态为FULL
            roomInfo.put("status", ROOM_STATUS_FULL);
            redisRoomManager.createRoom(roomId.toString(), (Map) roomInfo);
            return Result.error("房间已满");
        }
        Object statusObj = roomInfo.get("status");
        byte status = statusObj == null ? 0 : Byte.parseByte(statusObj.toString());
        if (status == ROOM_STATUS_FULL) {
            return Result.error("房间已满");
        }
        if (status == ROOM_STATUS_END) {
            return Result.error("房间已结束");
        }
        // 添加玩家详细信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("role", ROLE_PLAYER);
        userInfo.put("join_time", System.currentTimeMillis());
        // 查nickname
        User user = userMapper.selectById(userId);
        userInfo.put("nickname", user != null ? user.getNickname() : "");
        redisRoomManager.addRoomPlayer(roomId.toString(), userId.toString(), userInfo);
        // 写入反向索引，便于O(1)查询当前房间
        redisRoomManager.getRedisTemplate().opsForValue().set("user:" + userId + ":currentRoom", roomId.toString());
        return Result.success("加入房间成功", roomId);
    }
} 