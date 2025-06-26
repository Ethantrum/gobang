package com.example.gobang.pojo.vo.websocket;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ErrorVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String message;
}
