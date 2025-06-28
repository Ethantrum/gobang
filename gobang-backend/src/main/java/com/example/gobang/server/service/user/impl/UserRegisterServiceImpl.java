package com.example.gobang.server.service.user.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserRegisterDTO;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.service.user.UserRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserRegisterServiceImpl implements UserRegisterService {
    private final UserMapper userMapper;
    @Override
    public Result register(UserRegisterDTO userRegisterDTO) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userRegisterDTO.getUsername());
        if (userMapper.selectOne(queryWrapper) != null) {
            return Result.error("用户名已存在");
        }
        // 2. 检查昵称是否已存在
        queryWrapper.clear();
        queryWrapper.eq(User::getNickname, userRegisterDTO.getNickname());
        if (userMapper.selectOne(queryWrapper) != null) {
            return Result.error("昵称已存在");
        }
        // 3. 检查邮箱是否已存在
        queryWrapper.clear();
        queryWrapper.eq(User::getEmail, userRegisterDTO.getEmail());
        if (userMapper.selectOne(queryWrapper)!= null) {
            return Result.error("邮箱已存在");
        }

        // 4. 加密密码
        String hashedPassword = BCrypt.hashpw(userRegisterDTO.getPassword().trim());

        // 5. 注册新用户
        User user = User.builder()
                .username(userRegisterDTO.getUsername())
                .nickname(userRegisterDTO.getNickname())
                .password(hashedPassword)
                .email(userRegisterDTO.getEmail())
                .createTime(LocalDateTime.now())
                .build();

        // 6. 保存用户
        Integer count  = userMapper.insert(user);
        if (count < 0){
            return Result.error("注册失败");
        }
        return Result.success("注册成功");
    }
}
