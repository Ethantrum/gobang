package com.example.gobang.server.service;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomLeaveDTO;
import com.example.gobang.server.controller.room.RoomLeaveController;

public interface RoomService {
    Result roomList(int pageNum, int pageSize);

    Result searchRoom(String keyword, int pageNum, int pageSize);

    Result roomCreate(Long userId);

    Result roomJoin(Long roomId, Long userId);

    Result roomCurrentRoom(Long userId);

    Result roomLeave(RoomLeaveDTO roomLeaveDTO);
}
