package com.example.gobang.server.service.room.impl;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.vo.room.RoomListVO;
import com.example.gobang.server.service.room.RoomSearchService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 房间搜索服务实现：支持按房间ID、房主ID模糊搜索房间。
 * 只统计玩家人数，分页返回结果。
 */
@Service
public class RoomSearchServiceImpl implements RoomSearchService {
    @Autowired
    private RedisRoomManager redisRoomManager;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 搜索房间。
     * @param keyword 关键字（可为空）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 搜索结果，包含房间列表和总数
     */
    @Override
    public Result searchRoom(String keyword, int pageNum, int pageSize) {
        Object maxIdObj = redisTemplate.opsForValue().get("room:id:incr");
        long maxId = 0;
        if (maxIdObj != null) {
            try {
                maxId = Long.parseLong(maxIdObj.toString());
            } catch (Exception ignored) {}
        }
        List<Long> matchedRoomIds = new ArrayList<>();
        for (long i = 1; i <= maxId; i++) {
            String roomKey = "room:" + i;
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(roomKey))) continue;
            Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(String.valueOf(i));
            if (roomInfo == null || roomInfo.isEmpty()) continue;
            boolean match = false;
            if (keyword == null || keyword.isEmpty()) {
                match = true;
            } else {
                // 支持按roomId、ownerId模糊查找
                if (String.valueOf(i).contains(keyword)) match = true;
                Object ownerIdObj = roomInfo.get("owner_id");
                if (ownerIdObj != null && ownerIdObj.toString().contains(keyword)) match = true;
            }
            if (match) matchedRoomIds.add(i);
        }
        int total = matchedRoomIds.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Long> pageRoomIds = matchedRoomIds.subList(fromIndex, toIndex);
        List<RoomListVO> roomList = pageRoomIds.stream().map(roomId -> {
            Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomId.toString());
            Object ownerIdObj = roomInfo.get("owner_id");
            Object statusObj = roomInfo.get("status");
            Set<Object> players = redisRoomManager.getRoomPlayerIds(roomId.toString());
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
        return Result.success("搜索成功", data);
    }
} 