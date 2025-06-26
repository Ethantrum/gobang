package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomLeaveDTO;
import com.example.gobang.server.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomLeaveController {
    @Autowired
    private RoomService roomService;

    @PostMapping("/api/room/leave")
    public Result roomLeave(@Validated @RequestBody RoomLeaveDTO roomLeaveDTO) {
        return roomService.roomLeave(roomLeaveDTO);
    }
}
