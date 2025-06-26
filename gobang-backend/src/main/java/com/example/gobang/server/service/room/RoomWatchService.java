package com.example.gobang.server.service.room;

import com.example.gobang.common.result.Result;

public interface RoomWatchService {
    Result roomWatch(Long roomId, Long userId);
}
