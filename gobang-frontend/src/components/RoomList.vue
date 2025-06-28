<template>
  <div class="room-list">
    <template v-if="rooms.length > 0">
      <div class="room-item" v-for="room in rooms" :key="room.roomId">
        <span>房间号: <b>{{ room.roomId }}</b></span>
        <span>房主ID: {{ room.ownerId }}</span>
        <span>玩家: {{ room.count }}/2</span>
        <span>状态: {{ getStatusText(room.status) }}</span>
        <button v-if="room.count < 2" @click="$emit('join-room', room.roomId)" :disabled="joinRoomLoading[room.roomId]">{{ joinRoomLoading[room.roomId] ? '加入中...' : '加入' }}</button>
        <button v-else-if="room.count === 2 && room.status > 0" @click="$emit('watch-room', room.roomId)" :disabled="watchRoomLoading[room.roomId]">{{ watchRoomLoading[room.roomId] ? '观战中...' : '观战' }}</button>
        <span v-else class="waiting-text">等待中...</span>
      </div>
    </template>
    <template v-else>
      <div class="no-room-tip">暂无房间，快来创建吧！</div>
    </template>
  </div>
</template>

<script>
export default {
  name: 'RoomList',
  props: {
    rooms: {
      type: Array,
      required: true
    },
    joinRoomLoading: {
      type: Object,
      default: () => ({})
    },
    watchRoomLoading: {
      type: Object,
      default: () => ({})
    }
  },
  methods: {
    getStatusText(status) {
      switch (status) {
        case 0: return '等待'
        case 1: return '开始'
        case 2: return '结束'
        default: return '未知'
      }
    }
  }
}
</script>

<style scoped>
.room-list {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 120px;
}
.room-item {
  display: flex;
  gap: 32px;
  align-items: center;
  padding: 18px 24px;
  border-radius: 12px;
  background: #f9f9f9;
  box-shadow: 0 2px 8px rgba(45,140,240,0.08);
  font-size: 17px;
  border: 1px solid #e6e6e6;
  transition: box-shadow 0.2s, border 0.2s;
}
.room-item:hover {
  box-shadow: 0 4px 16px rgba(45,140,240,0.16);
  border: 1.5px solid #2d8cf0;
}
.room-item span {
  min-width: 90px;
  color: #333;
}
.room-item button {
  padding: 10px 24px;
  font-size: 16px;
  background: #4CAF50;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}
.room-item button:disabled {
  background: #bbb;
  cursor: not-allowed;
}
.room-item button:hover:not(:disabled) {
  background: #388e3c;
}
.room-item button:last-child {
  background: #2d8cf0;
}
.room-item button:last-child:hover {
  background: #1769aa;
}
.waiting-text {
  color: #999;
  font-style: italic;
  min-width: auto !important;
}
.no-room-tip {
  text-align: center;
  color: #aaa;
  font-size: 20px;
  padding: 40px 0 30px 0;
  letter-spacing: 2px;
}
</style> 