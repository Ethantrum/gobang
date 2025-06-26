package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.pojo.vo.room.RoomListVO;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.service.room.RoomListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoomListServiceImpl implements RoomListService {
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;

    @Override
    public Result roomList(int pageNum, int pageSize) {
        Page<Room> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Room> roomQueryWrapper = new LambdaQueryWrapper<>();
        Page<Room> roomPage = roomMapper.selectPage(page, roomQueryWrapper);
        List<RoomListVO> roomList = roomPage.getRecords().stream().map(room -> {
            LambdaQueryWrapper<RoomUser> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(RoomUser::getRoomId, room.getRoomId());
            userQueryWrapper.in(RoomUser::getRole, 0, 1, 2);
            long memberCount = roomUserMapper.selectCount(userQueryWrapper);
            return RoomListVO.builder()
                    .roomId(room.getRoomId())
                    .ownerId(room.getOwnerId())
                    .count(memberCount)
                    .status(room.getStatus())
                    .build();
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("total", roomPage.getTotal());
        data.put("list", roomList);
        return Result.success("获取成功", data);
    }
} 