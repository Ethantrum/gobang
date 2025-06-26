package com.example.gobang.server.service.user;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserLoginDTO;

public interface UserLoginService {
    Result login(UserLoginDTO userLoginDTO);
}
