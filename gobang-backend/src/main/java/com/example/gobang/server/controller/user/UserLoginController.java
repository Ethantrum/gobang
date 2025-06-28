package com.example.gobang.server.controller.user;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserLoginDTO;
import com.example.gobang.server.service.user.UserLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserLoginController {
    private final UserLoginService userLoginService;

    @PostMapping("/api/user/login")
    public Result login(@Validated @RequestBody UserLoginDTO userLoginDTO){
        return userLoginService.login(userLoginDTO);
    }
}
