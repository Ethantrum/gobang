package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomWatchDTO;
import com.example.gobang.server.service.room.RoomWatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class RoomWatchController {
    @Autowired
    private RoomWatchService roomWatchService;

    @PostMapping("/api/room/watch")
    public Result roomWatch(@RequestBody RoomWatchDTO roomWatchDTO) {
        return roomWatchService.roomWatch(roomWatchDTO.getRoomId(), roomWatchDTO.getUserId());
    }

} 