package com.example.gobang.server.service.manage.room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Redis房间管理器，所有字段和结构完全模拟MySQL表设计。
 *
 * 1. 房间（room表）
 *    - Redis Hash: room:{room_id}
 *      - owner_id, status, create_time
 *
 * 2. 房间成员（room_user表）
 *    - Redis Set: room:{room_id}:users  // 存user_id
 *    - Redis Hash: room:{room_id}:user:{user_id}  // role, join_time
 *
 * 3. 对局记录（game_record表）
 *    - Redis Hash: game:{game_id}
 *      - room_id, black_id, white_id, winner, start_time, end_time
 *
 * 4. 对局落子（game_moves表）
 *    - Redis List: game:{game_id}:moves
 *      - 每个元素为Map（或JSON），包含 move_index, x, y, player, move_time
 */
@Component
public class RedisRoomManager {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ---------------- 房间相关 ----------------
    /**
     * 创建房间（对应MySQL room表）
     * Redis Hash: room:{room_id}
     * 字段：owner_id, status, create_time
     */
    public void createRoom(String roomId, Map<String, Object> roomInfo) {
        redisTemplate.opsForHash().putAll("room:" + roomId, roomInfo);
    }

    /**
     * 获取房间信息（对应MySQL room表）
     */
    public Map<Object, Object> getRoomInfo(String roomId) {
        return redisTemplate.opsForHash().entries("room:" + roomId);
    }

    /**
     * 删除房间（对应MySQL room表）
     * 只做数据清理，不做通知
     */
    public void deleteRoom(String roomId) {
        // 先发送kick消息给所有玩家和观战者
        sendKickMessageToAllUsers(roomId, "房间已解散");
        
        // 删除房间属性
        redisTemplate.delete("room:" + roomId);

        // 删除所有玩家及其详细信息，并清理反向索引
        Set<Object> playerIds = redisTemplate.opsForSet().members("room:" + roomId + ":players");
        if (playerIds != null) {
            for (Object userId : playerIds) {
                redisTemplate.delete("room:" + roomId + ":player:" + userId);
                redisTemplate.delete("user:" + userId + ":currentRoom"); // 新增：清理反向索引
            }
        }
        redisTemplate.delete("room:" + roomId + ":players");

        // 删除所有观战者及其详细信息，并清理反向索引
        Set<Object> watcherIds = redisTemplate.opsForSet().members("room:" + roomId + ":watchers");
        if (watcherIds != null) {
            for (Object userId : watcherIds) {
                redisTemplate.delete("room:" + roomId + ":watcher:" + userId);
                redisTemplate.delete("user:" + userId + ":currentWatchRoom"); // 新增：清理反向索引
            }
        }
        redisTemplate.delete("room:" + roomId + ":watchers");

        // 删除该房间下所有对局及其落子
        Object maxGameIdObj = redisTemplate.opsForValue().get("game:id:incr");
        long maxGameId = 0L;
        if (maxGameIdObj != null) {
            try { maxGameId = Long.parseLong(maxGameIdObj.toString()); } catch (Exception ignored) {}
        }
        for (long gid = 1; gid <= maxGameId; gid++) {
            Map<Object, Object> game = getGameRecord(String.valueOf(gid));
            if (game == null || game.isEmpty()) continue;
            Object roomIdObj2 = game.get("room_id");
            if (roomIdObj2 != null && roomIdObj2.toString().equals(roomId)) {
                deleteGameRecord(String.valueOf(gid));
            }
        }
        
        // 删除重开缓存
        redisTemplate.delete("room:" + roomId + ":last_game");
    }

    /**
     * 发送kick消息给房间内所有用户
     * 这个方法需要在调用deleteRoom之前调用
     */
    private void sendKickMessageToAllUsers(String roomId, String reason) {
        // 这里我们通过Redis存储kick消息，让session管理器来读取并发送
        // 因为RedisRoomManager不应该直接依赖session管理器
        redisTemplate.opsForValue().set("room:" + roomId + ":kick_reason", reason, 5); // 5秒过期
    }

    // ---------------- 房间成员相关（重构：玩家与观战者分离） ----------------
    /**
     * 添加房间玩家
     * Redis Set: room:{room_id}:players  // 存player_id
     * Redis Hash: room:{room_id}:player:{user_id}  // role, join_time, nickname等
     */
    public void addRoomPlayer(String roomId, String userId, Map<String, Object> playerInfo) {
        redisTemplate.opsForSet().add("room:" + roomId + ":players", userId);
        redisTemplate.opsForHash().putAll("room:" + roomId + ":player:" + userId, playerInfo);
    }

    /**
     * 移除房间玩家
     */
    public void removeRoomPlayer(String roomId, String userId) {
        redisTemplate.opsForSet().remove("room:" + roomId + ":players", userId);
        redisTemplate.delete("room:" + roomId + ":player:" + userId);
    }

    /**
     * 获取房间所有玩家ID
     */
    public Set<Object> getRoomPlayerIds(String roomId) {
        return redisTemplate.opsForSet().members("room:" + roomId + ":players");
    }

    /**
     * 获取房间玩家详细信息
     */
    public Map<Object, Object> getRoomPlayerInfo(String roomId, String userId) {
        return redisTemplate.opsForHash().entries("room:" + roomId + ":player:" + userId);
    }

    /**
     * 添加房间观战者
     * Redis Set: room:{room_id}:watchers  // 存watcher_id
     * Redis Hash: room:{room_id}:watcher:{user_id}  // join_time, nickname等
     */
    public void addRoomWatcher(String roomId, String userId, Map<String, Object> watcherInfo) {
        redisTemplate.opsForSet().add("room:" + roomId + ":watchers", userId);
        redisTemplate.opsForHash().putAll("room:" + roomId + ":watcher:" + userId, watcherInfo);
    }

    /**
     * 移除房间观战者
     */
    public void removeRoomWatcher(String roomId, String userId) {
        redisTemplate.opsForSet().remove("room:" + roomId + ":watchers", userId);
        redisTemplate.delete("room:" + roomId + ":watcher:" + userId);
    }

    /**
     * 获取房间所有观战者ID
     */
    public Set<Object> getRoomWatcherIds(String roomId) {
        return redisTemplate.opsForSet().members("room:" + roomId + ":watchers");
    }

    /**
     * 获取房间观战者详细信息
     */
    public Map<Object, Object> getRoomWatcherInfo(String roomId, String userId) {
        return redisTemplate.opsForHash().entries("room:" + roomId + ":watcher:" + userId);
    }

    // ---------------- 对局记录相关 ----------------
    /**
     * 创建对局记录（对应MySQL game_record表）
     * Redis Hash: game:{game_id}
     * 字段：room_id, black_id, white_id, winner, start_time, end_time
     */
    public void createGameRecord(String gameId, Map<String, Object> gameInfo) {
        redisTemplate.opsForHash().putAll("game:" + gameId, gameInfo);
    }

    /**
     * 获取对局记录（对应MySQL game_record表）
     */
    public Map<Object, Object> getGameRecord(String gameId) {
        return redisTemplate.opsForHash().entries("game:" + gameId);
    }

    /**
     * 删除对局记录（对应MySQL game_record表）
     * 同时删除对局落子
     */
    public void deleteGameRecord(String gameId) {
        redisTemplate.delete("game:" + gameId);
        redisTemplate.delete("game:" + gameId + ":moves");
    }

    // ---------------- 对局落子相关 ----------------
    /**
     * 添加落子（对应MySQL game_moves表）
     * Redis List: game:{game_id}:moves
     * 每个元素为Map（或JSON），包含 move_index, x, y, player, move_time
     */
    public void addGameMove(String gameId, Map<String, Object> moveInfo) {
        redisTemplate.opsForList().rightPush("game:" + gameId + ":moves", moveInfo);
    }

    /**
     * 获取所有落子（对应MySQL game_moves表）
     */
    public List<Object> getGameMoves(String gameId) {
        return redisTemplate.opsForList().range("game:" + gameId + ":moves", 0, -1);
    }

    /**
     * 清空对局落子（对应MySQL game_moves表）
     */
    public void clearGameMoves(String gameId) {
        redisTemplate.delete("game:" + gameId + ":moves");
    }

    /**
     * 获取当前最大房间ID（自增值）
     */
    public Long getMaxRoomId() {
        Object maxIdObj = redisTemplate.opsForValue().get("room:id:incr");
        if (maxIdObj == null) return 0L;
        try {
            return Long.parseLong(maxIdObj.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 获取下一个对局ID（自增）
     */
    public Long getNextGameId() {
        return redisTemplate.opsForValue().increment("game:id:incr");
    }

    /**
     * 移除用户当前房间反向索引
     */
    public void removeUserCurrentRoom(String userId) {
        redisTemplate.delete("user:" + userId + ":currentRoom");
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
} 