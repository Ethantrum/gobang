package com.example.gobang.server.service;

import com.example.gobang.server.service.manage.room.RedisRoomManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 游戏归档服务
 * 负责将Redis中的游戏数据归档（目前只打印信息，为将来真正归档做准备）
 */
@Slf4j
@Service
public class GameArchiveService {

    @Autowired
    private RedisRoomManager redisRoomManager;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 归档指定房间和游戏ID的游戏记录
     * @param roomId 房间ID
     * @param gameId 游戏ID
     */
    public void archiveGame(Long roomId, Long gameId) {
        try {
            log.info("开始归档游戏 - roomId: {}, gameId: {}", roomId, gameId);
            
            // 1. 从Redis获取游戏记录
            Map<Object, Object> gameRecord = redisRoomManager.getGameRecord(gameId.toString());
            if (gameRecord == null || gameRecord.isEmpty()) {
                log.warn("游戏记录不存在，roomId: {}, gameId: {}", roomId, gameId);
                return;
            }

            // 2. 获取落子记录
            List<Object> moves = redisRoomManager.getGameMoves(gameId.toString());
            
            // 3. 构建归档数据对象
            GameArchiveData archiveData = buildArchiveData(roomId, gameId, gameRecord, moves);
            
            // 4. 打印归档信息
            log.info("=== 游戏归档信息 ===");
            log.info("归档数据: {}", archiveData);
            log.info("=== 归档完成 ===");

            // 5. 删除Redis中的游戏数据
            redisRoomManager.deleteGameRecord(gameId.toString());
            log.info("已删除Redis中的游戏数据 - gameId: {}", gameId);

        } catch (Exception e) {
            log.error("游戏归档异常，roomId: {}, gameId: {}", roomId, gameId, e);
        }
    }

    /**
     * 构建归档数据对象
     */
    private GameArchiveData buildArchiveData(Long roomId, Long gameId, Map<Object, Object> gameRecord, List<Object> moves) {
        GameArchiveData data = new GameArchiveData();
        data.setRoomId(roomId);
        data.setGameId(gameId);
        data.setBlackPlayerId(Long.valueOf(gameRecord.get("black_id").toString()));
        data.setWhitePlayerId(Long.valueOf(gameRecord.get("white_id").toString()));
        data.setWinner(gameRecord.get("winner") != null ? Long.valueOf(gameRecord.get("winner").toString()) : null);
        data.setStartTime(convertTimestampToString(gameRecord.get("start_time")));
        data.setEndTime(convertTimestampToString(gameRecord.get("end_time")));
        data.setTotalMoves(moves != null ? moves.size() : 0);
        
        // 构建落子序列
        List<MoveData> moveList = new ArrayList<>();
        if (moves != null) {
            for (int i = 0; i < moves.size(); i++) {
                Object moveObj = moves.get(i);
                if (moveObj instanceof Map) {
                    Map move = (Map) moveObj;
                    MoveData moveData = new MoveData();
                    moveData.setMoveIndex(i + 1);
                    moveData.setX(Integer.valueOf(move.get("x").toString()));
                    moveData.setY(Integer.valueOf(move.get("y").toString()));
                    moveData.setPlayer(Integer.valueOf(move.get("player").toString()));
                    moveData.setMoveTime(convertTimestampToString(move.get("move_time")));
                    moveList.add(moveData);
                }
            }
        }
        data.setMoves(moveList);
        
        return data;
    }

    /**
     * 将时间戳转换为可读的日期时间字符串
     */
    private String convertTimestampToString(Object timestamp) {
        if (timestamp == null) {
            return "null";
        }
        try {
            long ts = Long.parseLong(timestamp.toString());
            return dateFormat.format(new Date(ts));
        } catch (Exception e) {
            return timestamp.toString();
        }
    }

    /**
     * 游戏归档数据对象
     */
    @Data
    public static class GameArchiveData {
        private Long roomId;
        private Long gameId;
        private Long blackPlayerId;
        private Long whitePlayerId;
        private Long winner;
        private String startTime;
        private String endTime;
        private Integer totalMoves;
        private List<MoveData> moves;
    }

    /**
     * 落子数据对象
     */
    @Data
    public static class MoveData {
        private Integer moveIndex;
        private Integer x;
        private Integer y;
        private Integer player;
        private String moveTime;
    }
} 