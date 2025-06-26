package com.example.gobang.server.service.room;

import com.example.gobang.common.result.Result;

public interface RoomListService {
    Result roomList(int pageNum, int pageSize);
} 