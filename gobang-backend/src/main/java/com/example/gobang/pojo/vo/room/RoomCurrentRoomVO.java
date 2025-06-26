package com.example.gobang.pojo.vo.room;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class RoomCurrentRoomVO implements Serializable {
    private Long roomId;
    private Byte status;
}
