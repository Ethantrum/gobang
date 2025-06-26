package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.pojo.vo.room.RoomCurrentRoomVO;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.service.room.RoomCurrentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoomCurrentServiceImpl implements RoomCurrentService {
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;

    @Override
    public Result roomCurrentRoom(Long userId) {
        LambdaQueryWrapper<RoomUser> roomUserQueryWrapper = new LambdaQueryWrapper<>();
        roomUserQueryWrapper.eq(RoomUser::getUserId, userId);
        RoomUser roomUser = roomUserMapper.selectOne(roomUserQueryWrapper);
        if (roomUser == null) {
            return Result.error("用户未加入任何房间");
        }
        Room room = roomMapper.selectById(roomUser.getRoomId());
        RoomCurrentRoomVO roomCurrentRoomVO = RoomCurrentRoomVO.builder()
                .roomId(roomUser.getRoomId())
                .status(room.getStatus())
                .build();
        return Result.success(roomCurrentRoomVO);
    }
} 