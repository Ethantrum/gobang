package com.example.gobang.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gobang.pojo.entity.GameRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游戏记录Mapper
 */
@Mapper
public interface GameRecordMapper extends BaseMapper<GameRecord> {
} 