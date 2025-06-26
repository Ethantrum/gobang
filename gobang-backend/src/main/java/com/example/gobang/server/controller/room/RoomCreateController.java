package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomCreateDTO;
import com.example.gobang.server.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomCreateController {
    @Autowired
    private RoomService roomService;
    @PostMapping("/api/room/create")
    public Result roomCreate(@Validated @RequestBody RoomCreateDTO roomCreateDTO) {

        return roomService.roomCreate(roomCreateDTO.getUserId());
    }
}
