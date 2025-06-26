package com.example.gobang.server.controller.user;

import com.example.gobang.common.result.Result;
import com.example.gobang.pojo.dto.user.UserRegisterDTO;
import com.example.gobang.server.service.user.UserRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRegisterController {
    @Autowired
    private UserRegisterService userRegisterService;

    @PostMapping("/api/user/register")
    public Result register(@Validated @RequestBody UserRegisterDTO userRegisterDTO)
    {
        return userRegisterService.register(userRegisterDTO);
    }

}
