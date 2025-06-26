package com.example.gobang.pojo.vo.room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomListVO {
    private Long roomId;
    private Long ownerId;
    private Byte status;
    private Long count; // 房间成员数
}
