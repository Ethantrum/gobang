package com.example.gobang.server.service.user.impl;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.entity.User;
import com.example.gobang.pojo.vo.user.UserInfoVO;
import com.example.gobang.server.mapper.UserMapper;
import com.example.gobang.server.service.user.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Autowired
    private UserMapper userMapper;
    @Override
    public Result getUserInfo(Long userId) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if(user == null){
            return Result.error("用户不存在");
        }
        UserInfoVO userInfoVO = UserInfoVO.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
        return Result.success(userInfoVO);
    }
}
