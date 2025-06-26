package com.example.gobang.server.service.user.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.JWT.JwtUtils;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserLoginDTO;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.pojo.vo.user.UserLoginVO;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.service.user.UserLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserLoginServiceImpl implements UserLoginService {
    @Autowired
    private UserMapper userMapper;
    @Override
    public Result login(UserLoginDTO userLoginDTO) {
        //检查用户是否存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userLoginDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if(!BCrypt.checkpw(userLoginDTO.getPassword().trim(), user.getPassword())){
            return Result.error("密码错误");
        }
        UserLoginVO loginVO = UserLoginVO.builder()
                .userId(user.getUserId())
                .token(JwtUtils.generateToken(user.getUserId()))
                .build();
        return Result.success(loginVO);
    }
}
