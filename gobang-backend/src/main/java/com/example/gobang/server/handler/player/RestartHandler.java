package com.example.gobang.server.handler.player;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.mapper.GameRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RestartHandler implements WebSocketMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(RestartHandler.class);
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private GameRecordMapper gameRecordMapper;

    @WSMessageHandler("restart")
    public void handleRestart(WebSocketSession session, JSONObject data) {
        // 禁止前端直接发restart消息
        Long roomId = null;
        Long userId = null;
        if (session != null && session.getAttributes() != null) {
            Object roomIdObj = session.getAttributes().get("roomId");
            Object userIdObj = session.getAttributes().get("userId");
            if (roomIdObj instanceof Long) roomId = (Long) roomIdObj;
            if (userIdObj instanceof Long) userId = (Long) userIdObj;
        }
        if (roomId != null && userId != null) {
            playerSessionManager.sendToUser(roomId, userId, WSResult.error("禁止前端直接发restart消息"));
        }
    }
} 