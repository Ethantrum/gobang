package com.example.gobang.server.service;

import com.example.gobang.server.service.manage.room.RedisRoomManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 游戏重开缓存服务
 * 专门管理每个房间的上一局玩家信息，用于重开时交换黑白棋角色
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRestartCacheService {

    private final RedisRoomManager redisRoomManager;

    /**
     * 缓存上一局玩家信息
     * @param roomId 房间ID
     * @param gameId 游戏ID
     * @param blackPlayerId 黑棋玩家ID
     * @param whitePlayerId 白棋玩家ID
     */
    public void cacheLastGameInfo(Long roomId, Long gameId, Long blackPlayerId, Long whitePlayerId) {
        try {
            Map<String, Object> lastGameInfo = new HashMap<>();
            lastGameInfo.put("game_id", gameId);
            lastGameInfo.put("black_player_id", blackPlayerId);
            lastGameInfo.put("white_player_id", whitePlayerId);
            lastGameInfo.put("cache_time", System.currentTimeMillis());
            
            String cacheKey = "room:" + roomId + ":last_game";
            redisRoomManager.getRedisTemplate().opsForHash().putAll(cacheKey, lastGameInfo);
            
            log.info("已缓存上一局信息用于重开 - roomId: {}, gameId: {}, blackId: {}, whiteId: {}", 
                    roomId, gameId, blackPlayerId, whitePlayerId);
        } catch (Exception e) {
            log.error("缓存上一局信息失败 - roomId: {}, gameId: {}", roomId, gameId, e);
        }
    }

    /**
     * 获取上一局玩家信息
     * @param roomId 房间ID
     * @return 上一局信息，包含gameId、blackPlayerId、whitePlayerId
     */
    public Map<String, Object> getLastGameInfo(Long roomId) {
        try {
            String cacheKey = "room:" + roomId + ":last_game";
            Map<Object, Object> cacheData = redisRoomManager.getRedisTemplate().opsForHash().entries(cacheKey);
            
            if (cacheData == null || cacheData.isEmpty()) {
                return null;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("gameId", Long.valueOf(cacheData.get("game_id").toString()));
            result.put("blackPlayerId", Long.valueOf(cacheData.get("black_player_id").toString()));
            result.put("whitePlayerId", Long.valueOf(cacheData.get("white_player_id").toString()));
            result.put("cacheTime", Long.valueOf(cacheData.get("cache_time").toString()));
            
            log.info("获取到上一局信息 - roomId: {}, gameId: {}, blackId: {}, whiteId: {}", 
                    roomId, result.get("gameId"), result.get("blackPlayerId"), result.get("whitePlayerId"));
            
            return result;
        } catch (Exception e) {
            log.error("获取上一局信息失败 - roomId: {}", roomId, e);
            return null;
        }
    }

    /**
     * 清除房间的重开缓存
     * @param roomId 房间ID
     */
    public void clearLastGameInfo(Long roomId) {
        try {
            String cacheKey = "room:" + roomId + ":last_game";
            redisRoomManager.getRedisTemplate().delete(cacheKey);
            log.info("已清除重开缓存 - roomId: {}", roomId);
        } catch (Exception e) {
            log.error("清除重开缓存失败 - roomId: {}", roomId, e);
        }
    }

    /**
     * 检查是否有上一局信息
     * @param roomId 房间ID
     * @return 是否有上一局信息
     */
    public boolean hasLastGameInfo(Long roomId) {
        try {
            String cacheKey = "room:" + roomId + ":last_game";
            return redisRoomManager.getRedisTemplate().hasKey(cacheKey);
        } catch (Exception e) {
            log.error("检查重开缓存失败 - roomId: {}", roomId, e);
            return false;
        }
    }
} 