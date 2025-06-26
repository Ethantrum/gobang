package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomLeaveDTO;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.service.room.RoomLeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomLeaveServiceImpl implements RoomLeaveService {
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result roomLeave(RoomLeaveDTO roomLeaveDTO) {
        LambdaQueryWrapper<RoomUser> roomUserQueryWrapper = new LambdaQueryWrapper<>();
        roomUserQueryWrapper.eq(RoomUser::getRoomId, roomLeaveDTO.getRoomId());
        roomUserQueryWrapper.eq(RoomUser::getUserId, roomLeaveDTO.getUserId());
        RoomUser roomUser = roomUserMapper.selectOne(roomUserQueryWrapper);
        if (roomUser == null) {
            return Result.success("退出房间成功");
        }
        LambdaQueryWrapper<Room> roomQueryWrapper = new LambdaQueryWrapper<>();
        roomQueryWrapper.eq(Room::getRoomId, roomLeaveDTO.getRoomId());
        Room room = roomMapper.selectOne(roomQueryWrapper);
        if (room == null) {
            return Result.success("退出房间成功");
        }
        if (room.getOwnerId().equals(roomLeaveDTO.getUserId())) {
            List<RoomUser> leftUsers = roomUserMapper.selectList(
                new LambdaQueryWrapper<RoomUser>()
                    .eq(RoomUser::getRoomId, room.getRoomId())
                    .ne(RoomUser::getUserId, roomLeaveDTO.getUserId())
                    .orderByAsc(RoomUser::getJoinTime)
            );
            if (!leftUsers.isEmpty()) {
                RoomUser newOwner = leftUsers.get(0);
                room.setOwnerId(newOwner.getUserId());
                roomMapper.updateById(room);
            } else {
                roomMapper.deleteById(room.getRoomId());
            }
        }
        roomUserMapper.deleteById(roomUser);
        return Result.success("退出房间成功");
    }
} 