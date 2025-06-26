package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.constant.RoomStatusConstant;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.service.room.RoomJoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_END;
import static com.example.gobang.common.constant.RoomStatusConstant.ROOM_STATUS_FULL;
import static com.example.gobang.common.constant.RoomUserRoleConstant.ROLE_PLAYER;

@Service
public class RoomJoinServiceImpl implements RoomJoinService {

    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Override
    public Result roomJoin(Long roomId, Long userId) {
        Room room = roomMapper.selectById(roomId);
        LambdaQueryWrapper<RoomUser> roomUserQueryWrapper = new LambdaQueryWrapper<>();
        roomUserQueryWrapper.eq(RoomUser::getRoomId, roomId);
        roomUserQueryWrapper.eq(RoomUser::getUserId, userId);
        RoomUser roomUser = roomUserMapper.selectOne(roomUserQueryWrapper);
        if(roomUser != null){
            return Result.error("用户已经在房间中");
        }
        if (room.getStatus() == ROOM_STATUS_FULL) {
            return Result.error("房间已满");
        }
        if (room.getStatus() == ROOM_STATUS_END) {
            return Result.error("房间已结束");
        }
        RoomUser newRoomUser = RoomUser.builder()
               .roomId(roomId)
               .userId(userId)
               .role(ROLE_PLAYER)
               .joinTime(LocalDateTime.now())
               .build();
        roomUserMapper.insert(newRoomUser);
        return Result.success("加入房间成功", room.getRoomId());
    }
} 