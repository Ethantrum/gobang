package com.example.gobang.pojo.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;

@Data
public class UserLoginDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
