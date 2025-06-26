package com.example.gobang.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.gobang.pojo.entity.User;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT u.* FROM user u JOIN room_user ru ON u.user_id = ru.user_id WHERE ru.room_id = #{roomId}")
    List<User> selectUsersByRoomId(Long roomId);
}
