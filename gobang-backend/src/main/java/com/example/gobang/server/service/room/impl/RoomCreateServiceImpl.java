package com.example.gobang.server.service.room.impl;

import com.example.gobang.common.result.Result;
import com.example.gobang.server.service.room.RoomCreateService;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.pojo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_READY;

/**
 * 房间创建服务实现：处理房主创建房间的业务逻辑。
 * 自动处理观战身份切换、房间属性写入、玩家集合和反向索引。
 */
@Service
public class RoomCreateServiceImpl implements RoomCreateService {
    private final byte ROOM_STATUS_WAITING = 0;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    /**
     * 创建房间。
     * @param userId 房主用户ID
     * @return 创建结果，成功返回房间ID，失败返回错误信息
     */
    @Override
    public Result roomCreate(Long userId) {
        try {
            // 创建房间前，先移除观战身份及其反向索引，防止身份冲突
            redisTemplate.delete("user:" + userId + ":currentWatchRoom");
            redisTemplate.delete("room:" + userId + ":watcher:" + userId);

            // 查nickname
            User user = userMapper.selectById(userId);
            String nickname = user != null ? user.getNickname() : "";

            // 1. 自增生成房间ID
            Long roomId = redisTemplate.opsForValue().increment("room:id:incr");
            if (roomId == null) {
                return Result.error("创建房间失败，请稍后重试");
            }

            // 2. 写入房间属性
            Map<String, Object> roomInfo = new java.util.HashMap<>();
            roomInfo.put("owner_id", userId.toString());
            roomInfo.put("status", ROOM_STATUS_READY);
            roomInfo.put("create_time", System.currentTimeMillis());
            redisTemplate.opsForHash().putAll("room:" + roomId, roomInfo);

            // 3. 写入玩家集合和详细信息
            redisTemplate.opsForSet().add("room:" + roomId + ":players", userId.toString());
            Map<String, Object> userInfo = new java.util.HashMap<>();
            userInfo.put("role", ROLE_PLAYER);
            userInfo.put("join_time", System.currentTimeMillis());
            userInfo.put("nickname", nickname);
            redisTemplate.opsForHash().putAll("room:" + roomId + ":player:" + userId, userInfo);

            // 写入反向索引，便于O(1)查询当前房间
            redisTemplate.opsForValue().set("user:" + userId + ":currentRoom", roomId.toString());

            return Result.success("创建房间成功", roomId);
        } catch (Exception e) {
            // 捕获异常，返回友好提示
            return Result.error("创建房间失败，请稍后重试");
        }
    }
} 