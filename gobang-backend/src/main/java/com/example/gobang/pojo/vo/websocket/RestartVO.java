package com.example.gobang.pojo.vo.websocket;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class RestartVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
