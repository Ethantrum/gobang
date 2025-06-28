package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomCreateDTO;
import com.example.gobang.server.service.room.RoomCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomCreateController {
    private final RoomCreateService roomCreateService;
    @PostMapping("/api/room/create")
    public Result roomCreate(@Validated @RequestBody RoomCreateDTO roomCreateDTO) {
        return roomCreateService.roomCreate(roomCreateDTO.getUserId());
    }
}
