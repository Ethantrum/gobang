package com.example.gobang.pojo.dto.room;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class RoomWatchDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotBlank(message = "用户ID不能为空")
    private Long userId;
    @NotBlank(message = "房间ID不能为空")
    private Long roomId;
}
