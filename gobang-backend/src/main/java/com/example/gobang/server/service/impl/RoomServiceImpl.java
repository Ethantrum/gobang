package com.example.gobang.server.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomLeaveDTO;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.pojo.entity.GameRecord;
import com.example.gobang.pojo.entity.GameMoves;
import com.example.gobang.pojo.vo.room.RoomCurrentRoomVO;
import com.example.gobang.pojo.vo.room.RoomListVO;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.mapper.GameRecordMapper;
import com.example.gobang.server.mapper.GameMovesMapper;
import com.example.gobang.server.handler.WSSessionManager;
import com.example.gobang.common.result.WSResult;
import com.alibaba.fastjson.JSONObject;
import com.example.gobang.server.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {
    private final byte ROOM_STATUS_WAITING = 0;
    private final byte ROOM_STATUS_FULL = 1;
    private final byte ROOM_STATUS_END = 2;

    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private GameRecordMapper gameRecordMapper;
    @Autowired
    private GameMovesMapper gameMovesMapper;
    @Autowired
    private WSSessionManager wsSessionManager;

    @Override
    public Result roomList(int pageNum, int pageSize) {
        // 1. 查询房间分页数据
        Page<Room> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Room> roomQueryWrapper = new LambdaQueryWrapper<>();
        Page<Room> roomPage = roomMapper.selectPage(page, roomQueryWrapper);

        // 2. 查询每个房间的成员数
        List<RoomListVO> roomList = roomPage.getRecords().stream().map(room -> {
            // 查询房间成员数
            LambdaQueryWrapper<RoomUser> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(RoomUser::getRoomId, room.getRoomId());
            long memberCount = roomUserMapper.selectCount(userQueryWrapper);

            // 构建返回对象
            return RoomListVO.builder()
                    .roomId(room.getRoomId())
                    .ownerId(room.getOwnerId())
                    .count(memberCount)
                    .status(room.getStatus())
                    .build();
        }).collect(Collectors.toList());

        // 3. 构造返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("total", roomPage.getTotal());
        data.put("list", roomList);

        return Result.success("获取成功", data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result roomCreate(Long userId) {
        //room表中插入一条数据
        Room room = Room.builder()
                .ownerId(userId)
                .status(ROOM_STATUS_WAITING)
                .createTime(LocalDateTime.now())
                .build();
        roomMapper.insert(room);
        //room_user表中插入一条数据
        RoomUser roomUser = RoomUser.builder()
                .roomId(room.getRoomId())
                .userId(userId)
                .joinTime(LocalDateTime.now())
                .build();
        roomUserMapper.insert(roomUser);
        return Result.success("创建房间成功",room.getRoomId());
    }

    @Override
    public Result roomJoin(Long roomId, Long userId) {
        //判断房间是否存在
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            return Result.error("房间不存在");
        }
        //判断用户是否已经在房间中
        LambdaQueryWrapper<RoomUser> roomUserQueryWrapper = new LambdaQueryWrapper<>();
        roomUserQueryWrapper.eq(RoomUser::getRoomId, roomId);
        roomUserQueryWrapper.eq(RoomUser::getUserId, userId);
        RoomUser roomUser = roomUserMapper.selectOne(roomUserQueryWrapper);
        if(roomUser != null){
            return Result.error("您已经在房间中");
        }
        //判断房间是否已满
        if (room.getStatus() == ROOM_STATUS_FULL) {
            return Result.error("房间已满");
        }
        if (room.getStatus() == ROOM_STATUS_END) {
            return Result.error("对局已结束");
        }
        //将用户加入房间
        RoomUser newRoomUser = RoomUser.builder()
               .roomId(roomId)
               .userId(userId)
               .joinTime(LocalDateTime.now())
               .build();
        roomUserMapper.insert(newRoomUser);

        return Result.success("加入房间成功", room.getRoomId());
    }

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result roomLeave(RoomLeaveDTO roomLeaveDTO) {
        Long roomId = roomLeaveDTO.getRoomId();
        Long userId = roomLeaveDTO.getUserId();

        // 1. 验证用户和房间是否存在
        RoomUser roomUser = roomUserMapper.selectOne(new LambdaQueryWrapper<RoomUser>()
                .eq(RoomUser::getRoomId, roomId).eq(RoomUser::getUserId, userId));
        if (roomUser == null) {
            return Result.success("您已不在房间中");
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            roomUserMapper.deleteById(roomUser); // 清理脏数据
            return Result.success("房间已解散");
        }

        // 2. 从房间中移除用户
        roomUserMapper.deleteById(roomUser.getId());

        // 3. 根据情况更新房间状态
        List<RoomUser> remainingUsers = roomUserMapper.selectList(new LambdaQueryWrapper<RoomUser>()
                .eq(RoomUser::getRoomId, roomId).orderByAsc(RoomUser::getJoinTime));

        if (remainingUsers.isEmpty()) {
            // 没人了，删除房间和所有相关对局
            roomMapper.deleteById(roomId);
            gameRecordMapper.delete(new LambdaQueryWrapper<GameRecord>().eq(GameRecord::getRoomId, roomId));
        } else {
            // 还有人
            boolean wasGameInProgress = (room.getStatus() == 1);
            if (wasGameInProgress) {
                // 游戏进行中离开
                Long winnerId = remainingUsers.get(0).getUserId();
                GameRecord record = gameRecordMapper.selectOne(new LambdaQueryWrapper<GameRecord>()
                        .eq(GameRecord::getRoomId, roomId).isNull(GameRecord::getEndTime));

                if (record != null) {
                    record.setWinner(winnerId);
                    record.setEndTime(LocalDateTime.now());
                    gameRecordMapper.updateById(record);

                    // 核心修复：通过WebSocket广播结果
                    List<GameMoves> allMoves = gameMovesMapper.selectList(new LambdaQueryWrapper<GameMoves>().eq(GameMoves::getGameId, record.getId()));
                    int[][] board = buildBoardFromMoves(allMoves);
                    JSONObject resultData = new JSONObject();
                    resultData.put("winner", winnerId);
                    resultData.put("board", board);
                    wsSessionManager.broadcastToRoom(roomId, WSResult.result(resultData));
                }
                room.setStatus((byte) 0); // 房间状态重置为等待
            }
            
            // 检查并转让房主（仅在非游戏状态下或游戏结束后）
            if (!wasGameInProgress && room.getOwnerId().equals(userId)) {
                room.setOwnerId(remainingUsers.get(0).getUserId());
            }
            roomMapper.updateById(room);
        }
        return Result.success("退出房间成功");
    }

    private int[][] buildBoardFromMoves(List<GameMoves> moves) {
        int[][] board = new int[15][15];
        for (GameMoves move : moves) {
            board[move.getX()][move.getY()] = move.getPlayer();
        }
        return board;
    }

    @Override
    public Result searchRoom(String keyword, int pageNum, int pageSize) {
        Page<Room> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Room> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            // 找出昵称匹配的用户ID
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getNickname, keyword);
            List<Long> userIds = userMapper.selectList(userQuery).stream().map(User::getUserId).collect(Collectors.toList());
            // 搜索条件：房间号、房主ID、房主昵称
            query.like(Room::getRoomId, keyword)
                 .or().like(Room::getOwnerId, keyword);
            if (!userIds.isEmpty()) {
                query.or().in(Room::getOwnerId, userIds);
            }
        }
        Page<Room> roomPage = roomMapper.selectPage(page, query);
        // 查询每个房间的成员数
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
