package com.example.gobang.server.service;


import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserLoginDTO;
import com.example.gobang.pojo.dto.user.UserRegisterDTO;

public interface UserService {

    Result register(UserRegisterDTO userRegisterDTO);

    Result login(UserLoginDTO userLoginDTO);

    Result getUserInfo(Long userId);
}
