package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.server.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomSearchController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/api/room/search")
    public Result searchRoom(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "1") Integer pageNum,
        @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        return roomService.searchRoom(keyword, pageNum, pageSize);
    }
}
