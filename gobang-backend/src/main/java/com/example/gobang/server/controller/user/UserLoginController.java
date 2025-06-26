package com.example.gobang.server.controller.user;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserLoginDTO;
import com.example.gobang.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserLoginController {

    @Autowired
    private UserService userService;

    @PostMapping("/api/user/login")
    public Result login(@Validated @RequestBody UserLoginDTO userLoginDTO){
        System.out.println("登录请求");
        return userService.login(userLoginDTO);
    }
}
