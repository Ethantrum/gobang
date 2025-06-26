package com.example.gobang.server.service.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomLeaveDTO;

public interface RoomLeaveService {
    Result roomLeave(RoomLeaveDTO roomLeaveDTO);
} 