package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.service.room.RoomWatchService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class RoomWatchServiceImpl implements RoomWatchService {
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private RedisRoomManager redisRoomManager;
    @Autowired
    private UserMapper userMapper;

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
        if ((playerIds != null && playerIds.contains(userId.toString())) || (watcherIds != null && watcherIds.contains(userId.toString()))) {
            return Result.error("用户已经在房间中");
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
