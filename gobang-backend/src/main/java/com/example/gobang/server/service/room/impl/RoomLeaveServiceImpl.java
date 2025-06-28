package com.example.gobang.server.service.room.impl;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.room.RoomLeaveDTO;
import com.example.gobang.server.service.room.RoomLeaveService;
import com.example.gobang.server.service.manage.room.RedisRoomManager;
import com.example.gobang.server.handler.player.PlayerSessionManager;
import com.example.gobang.server.handler.watch.WatchSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 房间离开服务实现：处理用户离开房间或观战的业务逻辑。
 * 自动处理房主转让、房间解散、反向索引清理等。
 */
@Service
@RequiredArgsConstructor
public class RoomLeaveServiceImpl implements RoomLeaveService {
    private final RedisRoomManager redisRoomManager;

    private final PlayerSessionManager playerSessionManager;

    private final WatchSessionManager watchSessionManager;

    /**
     * 用户离开房间或观战。
     * @param roomLeaveDTO 离开请求参数，包含roomId和userId
     * @return 离开结果，成功返回提示，失败返回错误信息
     */
    @Override
    public Result roomLeave(RoomLeaveDTO roomLeaveDTO) {
        // --- Redis实现 ---
        String roomId = roomLeaveDTO.getRoomId().toString();
        String userId = roomLeaveDTO.getUserId().toString();
        Map<Object, Object> roomInfo = redisRoomManager.getRoomInfo(roomId);
        if (roomInfo == null || roomInfo.isEmpty()) {
            return Result.success("退出房间成功");
        }
        Set<Object> playerIds = redisRoomManager.getRoomPlayerIds(roomId);
        Set<Object> watcherIds = redisRoomManager.getRoomWatcherIds(roomId);
        if ((playerIds == null || !playerIds.contains(userId)) && (watcherIds == null || !watcherIds.contains(userId))) {
            return Result.success("退出房间成功");
        }
        // 判断身份
        if (playerIds != null && playerIds.contains(userId)) {
            Object ownerIdObj = roomInfo.get("owner_id");
            if (ownerIdObj != null && ownerIdObj.toString().equals(userId)) {
                // 当前用户是房主，转移房主或解散房间
                List<Object> leftUsers = new ArrayList<>(playerIds);
                leftUsers.remove(userId);
                if (!leftUsers.isEmpty()) {
                    String newOwnerId = leftUsers.get(0).toString();
                    roomInfo.put("owner_id", newOwnerId);
                    redisRoomManager.createRoom(roomId, (Map) roomInfo); // 更新房主
                } else {
                    // 先发送kick消息给所有用户
                    playerSessionManager.notifyAndCloseAllRoomSessions(roomLeaveDTO.getRoomId(), "房间已解散");
                    watchSessionManager.notifyAndCloseAllRoomSessions(roomLeaveDTO.getRoomId(), "房间已解散");
                    redisRoomManager.deleteRoom(roomId);
                    return Result.success("退出房间成功");
                }
            }
            redisRoomManager.removeRoomPlayer(roomId, userId);
            redisRoomManager.getRedisTemplate().delete("user:" + userId + ":currentRoom");
        } else if (watcherIds != null && watcherIds.contains(userId)) {
            redisRoomManager.removeRoomWatcher(roomId, userId);
            redisRoomManager.getRedisTemplate().delete("user:" + userId + ":currentWatchRoom");
        }
        return Result.success("退出房间成功");
    }
} 