package com.example.gobang.server.handler.watch;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gobang.pojo.entity.Room;
import com.example.gobang.pojo.entity.RoomUser;
import com.example.gobang.server.handler.WSMessageHandler;
import com.example.gobang.server.handler.WebSocketMessageHandler;
import com.example.gobang.server.mapper.RoomMapper;
import com.example.gobang.server.mapper.RoomUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.List;

@Component
public class WatchLeaveHandler implements WebSocketMessageHandler {
    @Autowired
    private WatchSessionManager watchSessionManager;
    @Autowired
    private RoomUserMapper roomUserMapper;
    @Autowired
    private RoomMapper roomMapper;

    @WSMessageHandler("watchLeave")
    public void handleWatchLeave(WebSocketSession session, Object data) {
        Long roomId = (Long) session.getAttributes().get("roomId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId == null || userId == null) {
            return;
        }
        // 1. 判断是否房主
        Room room = roomMapper.selectById(roomId);
        if (room != null && room.getOwnerId().equals(userId)) {
            // 查找剩余player
            List<RoomUser> leftUsers = roomUserMapper.selectList(
                new QueryWrapper<RoomUser>().eq("room_id", roomId).ne("user_id", userId)
            );
            RoomUser newOwner = null;
            for (RoomUser ru : leftUsers) {
                byte role = ru.getRole();
                if (role == 0 || role == 1 || role == 2) { // player, black, white
                    newOwner = ru;
                    break;
                }
            }
            if (newOwner != null) {
                room.setOwnerId(newOwner.getUserId());
                roomMapper.updateById(room);
            } else {
                // 没有player，删除房间
                roomMapper.deleteById(roomId);
            }
        } else {
            // 2. 不是房主则删除room_user表中自己信息
            roomUserMapper.delete(new QueryWrapper<RoomUser>().eq("room_id", roomId).eq("user_id", userId));
        }
        // 3. 移除观战session并断开
        watchSessionManager.removeWatchSession(session);
        try {
            session.close();
        } catch (Exception ignored) {}
    }
} 