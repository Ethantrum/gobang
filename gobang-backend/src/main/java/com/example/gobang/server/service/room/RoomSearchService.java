package com.example.gobang.server.service.room;

import com.example.gobang.common.result.Result;

public interface RoomSearchService {
    Result searchRoom(String keyword, int pageNum, int pageSize);
} 