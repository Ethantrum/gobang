package com.example.gobang.pojo.vo.user;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class UserLoginVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long userId;
    private String token;
}
