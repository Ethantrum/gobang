<template>
  <div class="game-hall">
    <!-- 消息提示条 -->
    <div v-if="showMessage" class="toast-message">{{ message }}</div>
    <!-- 用户信息区 -->
    <div class="user-info">
      <span>欢迎，{{ user.nickname }}</span>
      <span v-if="user.email">（{{ user.email }}）</span>
      <button @click="logout">退出登录</button>
    </div>
    <!-- 操作按钮区 -->
    <div class="actions">
      <button class="main-btn" @click="showCreateRoom = true" :disabled="createLoading">{{ createLoading ? '处理中...' : '创建房间' }}</button>
      <button @click="showJoinRoom = true" :disabled="joinLoading">{{ joinLoading ? '处理中...' : '加入房间' }}</button>
      <button @click="showSearchRoom = true" :disabled="searchLoading">{{ searchLoading ? '处理中...' : '搜索房间' }}</button>
      <button @click="fetchRooms" :disabled="refreshLoading">{{ refreshLoading ? '刷新中...' : '刷新' }}</button>
    </div>
    <!-- 房间列表区 -->
    <RoomList :rooms="rooms" :joinRoomLoading="joinRoomLoading" :watchRoomLoading="watchRoomLoading" @join-room="joinRoom" @watch-room="watchRoom" />
    <!-- 分页控件 -->
    <div v-if="rooms.length > 0" class="pagination">
      <button :disabled="pageNum === 1 || pageLoading" @click="handlePageChange(pageNum - 1)">{{ pageLoading ? '加载中...' : '上一页' }}</button>
      <span>第 {{ pageNum }} 页 / 共 {{ Math.ceil(total / pageSize) || 1 }} 页</span>
      <button :disabled="pageNum >= Math.ceil(total / pageSize) || pageLoading" @click="handlePageChange(pageNum + 1)">{{ pageLoading ? '加载中...' : '下一页' }}</button>
    </div>
    <!-- 创建房间弹窗 -->
    <div v-if="showCreateRoom" class="dialog-mask">
      <div class="dialog">
        <h3>创建房间</h3>
        <button @click="handleCreateRoom" :disabled="createLoading">{{ createLoading ? '创建中...' : '确认创建' }}</button>
        <button @click="showCreateRoom = false" :disabled="createLoading">取消</button>
      </div>
    </div>
    <!-- 加入房间弹窗 -->
    <div v-if="showJoinRoom" class="dialog-mask">
      <div class="dialog">
        <h3>加入房间</h3>
        <input v-model="joinRoomId" placeholder="请输入房间号" />
        <button @click="handleJoinRoom" :disabled="joinLoading">{{ joinLoading ? '加入中...' : '加入' }}</button>
        <button @click="showJoinRoom = false" :disabled="joinLoading">取消</button>
      </div>
    </div>
    <!-- 搜索房间弹窗 -->
    <div v-if="showSearchRoom" class="dialog-mask">
      <div class="dialog">
        <h3>搜索房间</h3>
        <input v-model="searchKey" placeholder="请输入房间号或房主" />
        <button @click="handleSearchRoom" :disabled="searchLoading">{{ searchLoading ? '搜索中...' : '搜索' }}</button>
        <button @click="showSearchRoom = false" :disabled="searchLoading">取消</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchRoomList, createRoom, joinRoomById, searchRoom, roomWatch } from '@/services/gameHallService'
import RoomList from '@/components/RoomList.vue'
import { globalState } from '@/globalState'
import { fetchUserInfo } from '@/services/userService'

export default {
  name: "GameHall",
  components: { RoomList },
  setup() {
    const router = useRouter()
    // 用户信息
    const user = globalState.user
    // 房间列表
    const rooms = ref([])
    const total = ref(0)
    const pageNum = ref(1)
    const pageSize = ref(10)
    // 弹窗控制
    const showCreateRoom = ref(false)
    const showJoinRoom = ref(false)
    const showSearchRoom = ref(false)
    // 加入房间输入
    const joinRoomId = ref('')
    // 搜索房间输入
    const searchKey = ref('')
    // 消息提示条
    const message = ref('')
    const showMessage = ref(false)
    let messageTimer = null
    const showToast = (msg, duration = 2000) => {
      message.value = msg
      showMessage.value = true
      if (messageTimer) clearTimeout(messageTimer)
      messageTimer = setTimeout(() => {
        showMessage.value = false
        message.value = ''
      }, duration)
    }
    // loading 状态
    const createLoading = ref(false)
    const joinLoading = ref(false)
    const searchLoading = ref(false)
    const refreshLoading = ref(false)
    const pageLoading = ref(false)
    const joinRoomLoading = reactive({})
    const watchRoomLoading = reactive({})

    // 获取房间列表
    const fetchRooms = async () => {
      refreshLoading.value = true
      try {
        const res = await fetchRoomList(pageNum.value, pageSize.value)
        if (res.code === 0 && res.data && Array.isArray(res.data.list)) {
          rooms.value = res.data.list
          total.value = res.data.total
        } else {
          rooms.value = []
          total.value = 0
          showToast(res.msg || '获取房间列表失败')
        }
      } catch (e) {
        rooms.value = []
        total.value = 0
        showToast('获取房间列表失败')
      } finally {
        refreshLoading.value = false
      }
    }

    // 创建房间
    const handleCreateRoom = async () => {
      createLoading.value = true
      try {
        const res = await createRoom()
        if (res.code === 0) {
          showToast('房间创建成功！')
          showCreateRoom.value = false
          fetchRooms()
          sessionStorage.setItem('roomId', res.data)
          // 跳转到五子棋页面，传递新创建的房间ID
          router.push({
            name: 'Gobang',
            params: { roomId: res.data }
          })
        } else {
          showToast(res.msg || '创建房间失败')
        }
      } catch (e) {
        showToast('创建房间失败')
      } finally {
        createLoading.value = false
      }
    }

    // 加入房间
    const handleJoinRoom = async () => {
      joinLoading.value = true
      if (!joinRoomId.value) return showToast('请输入房间号')
      try {
        const res = await joinRoomById(joinRoomId.value)
        if (res.code === 0) {
          showToast('加入房间成功！')
          showJoinRoom.value = false
          sessionStorage.setItem('roomId', joinRoomId.value)
          // 跳转到五子棋页面，传递加入的房间ID和身份类型
          router.push({
            name: 'Gobang',
            params: { roomId: joinRoomId.value, role: 'player' }
          })
        } else {
          showToast(res.msg || '加入房间失败')
        }
      } catch (e) {
        showToast('加入房间失败')
      } finally {
        joinLoading.value = false
      }
    }

    // 搜索房间
    const handleSearchRoom = async () => {
      searchLoading.value = true
      if (!searchKey.value) return showToast('请输入搜索内容')
      try {
        const res = await searchRoom(searchKey.value)
        if (res.code === 0 && res.data) {
          rooms.value = res.data.list
          total.value = res.data.total
        } else {
          rooms.value = []
          total.value = 0
          showToast(res.msg || '搜索失败')
        }
        showSearchRoom.value = false
      } catch (e) {
        showToast('搜索房间失败')
      } finally {
        searchLoading.value = false
      }
    }

    // 加入房间按钮
    const joinRoom = async (roomId) => {
      joinRoomLoading[roomId] = true
      try {
        const res = await joinRoomById(roomId)
        if (res.code === 0) {
          showToast('加入房间成功！')
          sessionStorage.setItem('roomId', roomId)
          // 跳转到五子棋页面，传递房间ID和身份类型
          router.push({
            name: 'Gobang',
            params: { roomId: roomId, role: 'player' }
          })
        } else {
          showToast(res.msg || '加入房间失败')
        }
      } catch (e) {
        showToast('加入房间失败')
      } finally {
        joinRoomLoading[roomId] = false
      }
    }

    // 观战按钮
    const watchRoom = async (roomId) => {
      watchRoomLoading[roomId] = true
      try {
        const res = await roomWatch(roomId)
        if (res.code === 0) {
          // 跳转到五子棋页面，传递房间ID和身份类型
          router.push({ name: 'Gobang', params: { roomId, role: 'watch' } })
        } else {
          showToast(res.msg || '观战失败')
        }
      } finally {
        watchRoomLoading[roomId] = false
      }
    }

    // 退出登录
    const logout = () => {
      localStorage.removeItem('username')
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      sessionStorage.removeItem('roomId')
      router.replace({ name: 'Login' })
    }

    // 分页切换
    const handlePageChange = async (newPage) => {
      pageLoading.value = true
      try {
        pageNum.value = newPage
        await fetchRooms()
      } finally {
        pageLoading.value = false
      }
    }

    // 获取状态文字
    const getStatusText = (status) => {
      switch (status) {
        case 0: return '等待'
        case 1: return '开始'
        case 2: return '结束'
        default: return '未知'
      }
    }

    onMounted(async () => {
      // 先拉取用户信息
      try {
        const res = await fetchUserInfo()
        if (res.code === 0 && res.data) {
          Object.assign(globalState.user, res.data)
        }
      } catch (e) {
        // eslint-disable-next-line no-empty
      }
      // 再拉取房间列表
      fetchRooms()
    })

    return {
      user,
      rooms,
      showCreateRoom,
      showJoinRoom,
      showSearchRoom,
      joinRoomId,
      searchKey,
      fetchRooms,
      handleCreateRoom,
      handleJoinRoom,
      handleSearchRoom,
      joinRoom,
      watchRoom,
      logout,
      handlePageChange,
      pageNum,
      pageSize,
      total,
      showMessage,
      message,
      getStatusText,
      createLoading,
      joinLoading,
      searchLoading,
      refreshLoading,
      pageLoading,
      joinRoomLoading,
      watchRoomLoading
    }
  }
}
</script>

<style scoped>
.game-hall {
  max-width: 700px;
  margin: 24px auto 0 auto;
  padding: 18px 12px 24px 12px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 16px rgba(0,0,0,0.10);
}
.user-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding: 14px 18px;
  background: #f7f7f7;
  border-radius: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #2d8cf0;
}
.user-info button {
  background: #f56c6c;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 10px 28px;
  cursor: pointer;
  font-size: 18px;
  transition: background 0.2s;
}
.user-info button:hover {
  background: #d9534f;
}
.actions {
  display: flex;
  gap: 24px;
  margin-bottom: 28px;
  justify-content: center;
}
.actions button {
  padding: 14px 36px;
  font-size: 18px;
  background: #2d8cf0;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(45,140,240,0.08);
}
.actions .main-btn {
  background: #4CAF50;
}
.actions button:hover {
  background: #1769aa;
  box-shadow: 0 4px 16px rgba(45,140,240,0.12);
}
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
.no-room-tip {
  text-align: center;
  color: #aaa;
  font-size: 20px;
  padding: 40px 0 30px 0;
  letter-spacing: 2px;
}
.dialog-mask {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.dialog {
  background: #fff;
  padding: 32px 36px;
  border-radius: 12px;
  min-width: 320px;
  box-shadow: 0 2px 16px rgba(45,140,240,0.08);
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.dialog h3 {
  margin: 0 0 10px 0;
  color: #2d8cf0;
  font-size: 20px;
}
.dialog input {
  padding: 8px 12px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 16px;
  margin-bottom: 8px;
}
.dialog button {
  padding: 8px 20px;
  font-size: 16px;
  background: #2d8cf0;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  margin-right: 10px;
  transition: background 0.2s;
}
.dialog button:hover {
  background: #1769aa;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  gap: 18px;
}
.pagination button {
  padding: 14px 32px;
  margin: 0 8px;
  border: 1.5px solid #2d8cf0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 18px;
  font-weight: bold;
  transition: background 0.2s, border 0.2s;
}
.pagination button:disabled {
  background: #eee;
  border: 1.5px solid #bbb;
  cursor: not-allowed;
}
.toast-message {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: #323232;
  color: #fff;
  padding: 12px 32px;
  border-radius: 6px;
  font-size: 16px;
  z-index: 9999;
  opacity: 0.95;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  transition: opacity 0.3s;
}
@media (max-width: 700px) {
  .game-hall {
    max-width: 98vw;
    padding: 10px;
  }
  .user-info {
    flex-direction: column;
    gap: 8px;
    font-size: 16px;
    padding: 10px;
  }
  .actions {
    flex-direction: column;
    gap: 10px;
  }
  .room-item {
    flex-direction: column;
    gap: 8px;
    font-size: 15px;
    padding: 10px 8px;
  }
  .dialog {
    min-width: 220px;
    padding: 18px 10px;
  }
}
</style> 