package com.example.gobang.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("game_records")
public class GameRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long gameId;
    private Long blackPlayerId;
    private Long whitePlayerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long winnerId;
    private String moves;
} 