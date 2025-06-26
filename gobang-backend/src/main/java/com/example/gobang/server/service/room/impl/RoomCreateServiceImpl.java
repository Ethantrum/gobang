package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.service.room.RoomCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RoomCreateServiceImpl implements RoomCreateService {
    private final byte ROOM_STATUS_WAITING = 0;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result roomCreate(Long userId) {
        Room room = Room.builder()
                .ownerId(userId)
                .status(ROOM_STATUS_WAITING)
                .createTime(LocalDateTime.now())
                .build();
        roomMapper.insert(room);
        RoomUser roomUser = RoomUser.builder()
                .roomId(room.getRoomId())
                .userId(userId)
                .joinTime(LocalDateTime.now())
                .build();
        roomUserMapper.insert(roomUser);
        return Result.success("创建房间成功", room.getRoomId());
    }
} 