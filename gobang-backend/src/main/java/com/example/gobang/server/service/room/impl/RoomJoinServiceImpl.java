package com.example.gobang.server.service.room.impl;

import com.alibaba.fastjson.JSON;
import com.example.gobang.common.result.Result;
import com.example.gobang.server.service.room.RoomJoinService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.server.handler.player.PlayerSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_END;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_FULL;
import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_MAX_PLAYER;

/**
 * 房间加入服务实现：处理用户加入房间的业务逻辑。
 * 只允许玩家身份加入，自动处理观战身份切换和反向索引。
 * 新增：使用Redis分布式锁防止高并发超员问题。
 */
@Service
@RequiredArgsConstructor
public class RoomJoinServiceImpl implements RoomJoinService {

    private static final Logger log = LoggerFactory.getLogger(RoomJoinServiceImpl.class);

    private final RedisRoomManager redisRoomManager;
    private final UserMapper userMapper;
    private final PlayerSessionManager playerSessionManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 用户加入房间。
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 加入结果，成功返回房间ID，失败返回错误信息
     */
    @Override
    public Result roomJoin(Long roomId, Long userId) {
        String lockKey = "room:join:lock:" + roomId;
        String lockValue = userId + ":" + System.currentTimeMillis();
        
        try {
            // 获取分布式锁，防止并发加入
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("用户{}尝试加入房间{}时获取锁失败", userId, roomId);
                return Result.error("房间正在处理其他请求，请稍后重试");
            }

            // 执行加入房间逻辑
            return doRoomJoin(roomId, userId);

        } catch (Exception e) {
            log.error("用户{}加入房间{}时发生异常", userId, roomId, e);
            return Result.error("加入房间失败，请稍后重试");
        } finally {
            // 释放分布式锁
            String currentLockValue = (String) redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 执行加入房间的具体逻辑
     */
    private Result doRoomJoin(Long roomId, Long userId) {
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
            // 检查用户是否有活跃的WebSocket连接
            if (playerSessionManager.hasActiveSession(roomId, userId)) {
                return Result.error("用户已经在房间中");
            } else {
                // 用户没有活跃连接，清理旧数据后重新加入
                log.info("用户{}在房间{}中没有活跃连接，清理旧数据后重新加入", userId, roomId);
                redisRoomManager.removeRoomPlayer(roomId.toString(), userId.toString());
                redisRoomManager.getRedisTemplate().delete("user:" + userId + ":currentRoom");
            }
        }

        // 判断房间人数是否已满
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
        
        log.info("用户{}成功加入房间{}", userId, roomId);
        return Result.success("加入房间成功", roomId);
    }
} 