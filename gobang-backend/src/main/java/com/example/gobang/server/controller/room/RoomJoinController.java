package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomJoinDTO;
import com.example.gobang.server.service.room.RoomJoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoomJoinController {
    private final RoomJoinService roomJoinService;
    @PostMapping("/api/room/join")
    public Result roomJoin(@Validated @RequestBody RoomJoinDTO roomJoinDTO)
    {
        return roomJoinService.roomJoin(roomJoinDTO.getRoomId(), roomJoinDTO.getUserId());
    }
}
