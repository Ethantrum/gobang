package com.example.gobang.pojo.dto.websocket;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MoveDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
