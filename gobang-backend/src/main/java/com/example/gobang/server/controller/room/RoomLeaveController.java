package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomLeaveDTO;
import com.example.gobang.server.service.room.RoomLeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomLeaveController {
    private final RoomLeaveService roomLeaveService;

    @PostMapping("/api/room/leave")
    public Result roomLeave(@Validated @RequestBody RoomLeaveDTO roomLeaveDTO) {
        return roomLeaveService.roomLeave(roomLeaveDTO);
    }
}
