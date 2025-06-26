package com.example.gobang.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("user") // 映射数据库表名
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) // 主键自增
    private Long userId;
    private String username; // 对应 VARCHAR(32) UNIQUE NOT NULL
    private String nickname; // 对应 VARCHAR(32) NOT NULL
    private String password; // 对应 VARCHAR(128) NOT NULL
    private String email;    // 对应 VARCHAR(64) UNIQUE NOT NULL
    private LocalDateTime createTime; // 对应 DATETIME DEFAULT CURRENT_TIMESTAMP
}