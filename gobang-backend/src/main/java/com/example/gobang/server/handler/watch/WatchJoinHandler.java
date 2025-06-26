package com.example.gobang.server.handler.watch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gobang.common.result.WSResult;
import com.example.gobang.pojo.entity.*;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import com.example.gobang.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.List;

@Component
public class WatchJoinHandler implements WebSocketMessageHandler {
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private UserMapper userMapper;

    @WSMessageHandler("watchJoin")
    public void handleWatchJoin(WebSocketSession session, JSONObject data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId == null || userId == null) {
            return;
        }
        watchSessionManager.registerWatchSession(roomId, userId, session);
        // 观战 join 逻辑
        List<RoomUser> roomUsers = roomUserMapper.selectList(
            new QueryWrapper<RoomUser>().eq("room_id", roomId).orderByAsc("id")
        );
        List<Long> userIds = roomUsers.stream().map(RoomUser::getUserId).toList();
        List<User> users = userMapper.selectBatchIds(userIds);
        List<RoomUser> players = roomUsers.stream().filter(ru -> "player".equals(ru.getRole())).toList();
        Long blackId = players.size() > 0 ? players.get(0).getUserId() : null;
        Long whiteId = players.size() > 1 ? players.get(1).getUserId() : null;
        JSONObject resp = new JSONObject();
        resp.put("players", users.stream().map(u -> {
            RoomUser ru = roomUsers.stream().filter(r -> r.getUserId().equals(u.getUserId())).findFirst().orElse(null);
            JSONObject ju = new JSONObject();
            ju.put("userId", u.getUserId());
            ju.put("nickname", u.getNickname());
            ju.put("isBlack", ru != null && "player".equals(ru.getRole()) && u.getUserId().equals(blackId));
            ju.put("isWhite", ru != null && "player".equals(ru.getRole()) && u.getUserId().equals(whiteId));
            return ju;
        }).toList());
        resp.put("blackId", blackId);
        resp.put("whiteId", whiteId);
        watchSessionManager.broadcastToRoom(roomId, WSResult.join(resp));
    }
} 