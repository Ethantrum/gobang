package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.service.room.RoomWatchService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import com.example.gobang.server.handler.player.PlayerSessionManager;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_END;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_FULL;
import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_WATCH;

/**
 * 观战服务实现：处理用户观战房间的业务逻辑。
 * 自动处理玩家身份切换和观战反向索引。
 */
@Service
@RequiredArgsConstructor
public class RoomWatchServiceImpl implements RoomWatchService {
    private static final Logger log = LoggerFactory.getLogger(RoomWatchServiceImpl.class);

    private final RedisRoomManager redisRoomManager;
    private final UserMapper userMapper;
    private final PlayerSessionManager playerSessionManager;
    private final WatchSessionManager watchSessionManager;

    /**
     * 用户加入观战。
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 加入结果，成功返回房间ID，失败返回错误信息
     */
    @Override
    public Result roomWatch(Long roomId, Long userId) {
        // 加入观战前，先移除玩家身份及其反向索引，防止重复身份
        redisRoomManager.removeRoomPlayer(roomId.toString(), userId.toString());
        redisRoomManager.getRedisTemplate().delete("user:" + userId + ":currentRoom");
        Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomId.toString());
        Set<Object> watcherIds = redisRoomManager.getRoomWatcherIds(roomId.toString());
        
        // 检查用户是否已在房间中
        boolean isPlayer = playerIds != null && playerIds.contains(userId.toString());
        boolean isWatcher = watcherIds != null && watcherIds.contains(userId.toString());
        
        if (isPlayer || isWatcher) {
            // 检查用户是否有活跃的WebSocket连接
            boolean hasActivePlayerSession = isPlayer && playerSessionManager.hasActiveSession(roomId, userId);
            boolean hasActiveWatchSession = isWatcher && watchSessionManager.hasActiveSession(roomId, userId);
            
            if (hasActivePlayerSession || hasActiveWatchSession) {
                return Result.error("用户已经在房间中");
            } else {
                // 用户没有活跃连接，清理旧数据后重新加入
                log.info("用户{}在房间{}中没有活跃连接，清理旧数据后重新观战", userId, roomId);
                if (isPlayer) {
                    redisRoomManager.removeRoomPlayer(roomId.toString(), userId.toString());
                    redisRoomManager.getRedisTemplate().delete("user:" + userId + ":currentRoom");
                }
                if (isWatcher) {
                    redisRoomManager.removeRoomWatcher(roomId.toString(), userId.toString());
                    redisRoomManager.getRedisTemplate().delete("user:" + userId + ":currentWatchRoom");
                }
            }
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("role", ROLE_WATCH);
        userInfo.put("join_time", System.currentTimeMillis());
        User user = userMapper.selectById(userId);
        userInfo.put("nickname", user != null ? user.getNickname() : "");
        redisRoomManager.addRoomWatcher(roomId.toString(), userId.toString(), userInfo);
        // 写入观战反向索引，便于O(1)查询当前观战房间
        redisRoomManager.getRedisTemplate().opsForValue().set("user:" + userId + ":currentWatchRoom", roomId.toString());
        return Result.success("加入房间成功", roomId);
    }
}
