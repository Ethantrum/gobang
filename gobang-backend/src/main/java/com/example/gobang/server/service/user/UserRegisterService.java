package com.example.gobang.server.service.user;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserRegisterDTO;

public interface UserRegisterService {
    Result register(UserRegisterDTO userRegisterDTO);
}
