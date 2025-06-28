package com.example.gobang.server.service.room.impl;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.vo.room.RoomListVO;
import com.example.gobang.server.service.room.RoomListService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间列表服务实现：分页获取所有房间及其玩家人数、状态等信息。
 */
@Service
@RequiredArgsConstructor
public class RoomListServiceImpl implements RoomListService {
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取房间列表。
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 房间列表和总数
     */
    @Override
    public Result roomList(int pageNum, int pageSize) {
        Object maxIdObj = redisTemplate.opsForValue().get("room:id:incr");
        long maxId = 0;
        if (maxIdObj != null) {
            try {
                maxId = Long.parseLong(maxIdObj.toString());
            } catch (Exception ignored) {}
        }
        List<Long> allRoomIds = new ArrayList<>();
        for (long i = 1; i <= maxId; i++) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey("room:" + i))) {
                allRoomIds.add(i);
            }
        }
        int total = allRoomIds.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Long> pageRoomIds = allRoomIds.subList(fromIndex, toIndex);
        List<RoomListVO> roomList = pageRoomIds.stream().map(roomId -> {
            Map<Object, Object> roomInfo = redisTemplate.opsForHash().entries("room:" + roomId);
            Object ownerIdObj = roomInfo.get("owner_id");
            Object statusObj = roomInfo.get("status");
            Set<Object> players = redisTemplate.opsForSet().members("room:" + roomId + ":players");
            long count = players != null ? players.size() : 0;
            return RoomListVO.builder()
                    .roomId(roomId)
                    .ownerId(ownerIdObj == null ? null : Long.valueOf(ownerIdObj.toString()))
                    .status(statusObj == null ? null : Byte.valueOf(statusObj.toString()))
                    .count(count)
                    .build();
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("list", roomList);
        return Result.success("获取成功", data);
    }
} 