package com.example.gobang.server.service.room.impl;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.vo.room.RoomCurrentRoomVO;
import com.example.gobang.server.service.room.RoomCurrentService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 当前房间查询服务实现：O(1)高效查询用户当前所在房间。
 */
@Service
@RequiredArgsConstructor
public class RoomCurrentServiceImpl implements RoomCurrentService {

    private final RedisRoomManager redisRoomManager;

    /**
     * 查询用户当前所在房间。
     * @param userId 用户ID
     * @return 当前房间信息，未加入房间则返回错误
     */
    @Override
    public Result roomCurrentRoom(Long userId) {
        // 直接查反向索引，O(1)高效实现
        String roomId = (String) redisRoomManager.getRedisTemplate().opsForValue().get("user:" + userId + ":currentRoom");
        if (roomId != null) {
            Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomId);
            Object statusObj = roomInfo.get("status");
            RoomCurrentRoomVO vo = RoomCurrentRoomVO.builder()
                    .roomId(Long.valueOf(roomId))
                    .status(statusObj == null ? null : Byte.valueOf(statusObj.toString()))
                    .build();
            return Result.success(vo);
        }
        return Result.error("用户未加入任何房间");
    }
} 