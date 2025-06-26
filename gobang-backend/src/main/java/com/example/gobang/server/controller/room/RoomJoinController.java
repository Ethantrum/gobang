package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomJoinDTO;
import com.example.gobang.server.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomJoinController {
    @Autowired
    private RoomService roomService;
    @PostMapping("/api/room/join")
    public Result roomJoin(@Validated @RequestBody RoomJoinDTO roomJoinDTO)
    {
        return roomService.roomJoin(roomJoinDTO.getRoomId(), roomJoinDTO.getUserId());
    }
}
