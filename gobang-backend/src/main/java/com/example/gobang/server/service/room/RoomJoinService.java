package com.example.gobang.server.service.room;

import com.example.gobang.common.result.Result;

public interface RoomJoinService {
    Result roomJoin(Long roomId, Long userId);
} 