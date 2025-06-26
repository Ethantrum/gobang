package com.example.gobang.server.service.room.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.pojo.vo.room.RoomListVO;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.service.room.RoomSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoomSearchServiceImpl implements RoomSearchService {
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result searchRoom(String keyword, int pageNum, int pageSize) {
        Page<Room> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Room> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getNickname, keyword);
            List<Long> userIds = userMapper.selectList(userQuery).stream().map(User::getUserId).collect(Collectors.toList());
            query.like(Room::getRoomId, keyword)
                 .or().like(Room::getOwnerId, keyword);
            if (!userIds.isEmpty()) {
                query.or().in(Room::getOwnerId, userIds);
            }
        }
        Page<Room> roomPage = roomMapper.selectPage(page, query);
        List<RoomListVO> roomList = roomPage.getRecords().stream().map(room -> {
            LambdaQueryWrapper<RoomUser> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(RoomUser::getRoomId, room.getRoomId());
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
        return Result.success("搜索成功", data);
    }
} 