package com.example.gobang.server.controller.room;

import com.example.gobang.common.result.Result;
import com.example.gobang.server.service.room.RoomListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomListController {
    @Autowired
    private RoomListService roomListService;

    // 获取房间列表的方法
    @GetMapping("/api/room/list")
    public Result roomList(int pageNum, int pageSize) {
        return roomListService.roomList(pageNum, pageSize);
    }
}
