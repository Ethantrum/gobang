<template>
  <div class="gobang-container">
    <!-- 顶部房间信息区 -->
    <header class="room-header">
      <span>房间号: <b>{{ roomId }}</b></span>
      <span>你的身份: <b :class="myPieceClass">{{ myPieceText }}</b></span>
      <span>当前用户: {{ user.nickname }}</span>
      <button class="exit-btn" @click="onLeaveClick" :disabled="leaveLoading">{{ leaveLoading ? '退出中...' : '退出房间' }}</button>
    </header>

    <!-- 棋盘与回合提示区 -->
    <main class="board-section">
      <div v-if="isWatcher" class="watcher-banner watcher-indicator">观战中：您当前仅可观战，无法操作棋盘</div>
      <div class="turn-indicator" :class="{ active: isMyTurn && playerCount === 2 && !winner }">
        <template v-if="!isWatcher">{{ indicatorText }}</template>
      </div>
      <div class="board">
        <div v-for="(row, rowIndex) in 15" :key="rowIndex" class="row">
          <div v-for="(col, colIndex) in 15" :key="colIndex" class="cell" @click="placePiece(rowIndex, colIndex)">
            <div v-if="boardData[rowIndex][colIndex]"
                 :class="[
                     boardData[rowIndex][colIndex] === 1 ? 'black-piece' : 'white-piece',
                     'piece',
                     { 'highlight': isWinningPiece(rowIndex, colIndex) }
                 ]">
            </div>
          </div>
        </div>
      </div>
    </main>
    <!-- Toast提示挂载点 -->
    <div v-if="toastMsg" class="toast-message">{{ toastMsg }}</div>
    <div v-if="showLeaveConfirm" class="restart-dialog-mask">
      <div class="restart-dialog">
        <div class="restart-title">确定要退出房间吗？</div>
        <div class="restart-actions">
          <button @click="confirmLeaveRoom" :disabled="leaveLoading">确定</button>
          <button @click="showLeaveConfirm = false" :disabled="leaveLoading">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { connectWs, closeWs, sendWs } from '@/services/gobangGameService'
import { leaveRoom } from '@/services/gameHallService'
import { globalState } from '@/globalState'

export default {
  name: 'GobangWatch',
  components: { },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const boardData = ref(Array(15).fill().map(() => Array(15).fill(0)))
    const currentPlayer = ref(1)
    const socket = ref(null)
    const wsStatus = ref('已断开')
    const winner = ref(null)
    const winningLine = ref([])
    const players = ref([])
    const toastMsg = ref('')
    const roomId = ref(route.params.roomId || sessionStorage.getItem('roomId') || 'null')
    const user = globalState.user
    const roomInfo = ref({ status: 0, count: 0 })
    const userId = ref(localStorage.getItem('userId') || 'user_' + Math.floor(Math.random() * 10000))
    if (!user.userId && userId.value) {
      user.userId = userId.value
    }
    const leaveLoading = ref(false)
    const isWatcher = ref(true) // 观战端始终为true
    const showLeaveConfirm = ref(false)

    const myPieceText = computed(() => {
      const me = players.value.find(p => p.userId == user.userId)
      if (!me) return '观战'
      if (me.isWatcher) return '观战'
      if (me.isBlack) return '黑棋'
      if (me.isWhite) return '白棋'
      return '观战'
    })
    const myPieceClass = computed(() => {
      const me = players.value.find(p => p.userId == user.userId)
      if (!me) return 'spectator'
      if (me.isWatcher) return 'watcher-label'
      if (me.isBlack) return 'black-piece-label'
      if (me.isWhite) return 'white-piece-label'
      return 'spectator'
    })
    const isMyTurn = computed(() => false)
    const playerCount = computed(() => players.value.filter(p => !p.isWatcher).length)
    const winnerText = computed(() => {
      if (!winner.value) return ''
      const winPlayer = players.value.find(p => p.userId == winner.value)
      if (winPlayer) {
        return `游戏结束，${winPlayer.nickname} 获胜！`
      }
      return '对局结束'
    })
    function syncIsWatcher() {
      isWatcher.value = true
    }
    const indicatorText = computed(() => {
      return '观战中：您当前仅可观战，无法操作棋盘'
    })
    function showToast(msg) {
      toastMsg.value = msg
      setTimeout(() => { toastMsg.value = '' }, 2000)
    }
    const onLeaveClick = () => {
      showLeaveConfirm.value = true
    }
    const confirmLeaveRoom = async () => {
      showLeaveConfirm.value = false
      leaveLoading.value = true
      try {
        const res = await leaveRoom(roomId.value)
        if (res.code === 0) {
          sessionStorage.removeItem('roomId')
          router.push({ name: 'GameHall' })
        } else {
          showToast(res.msg || '退出房间失败')
        }
      } finally {
        leaveLoading.value = false
      }
    }
    const onWSMessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        switch (msg.type) {
          case 'join':
            players.value = msg.data.players
            syncIsWatcher()
            break
          case 'start': {
            players.value = msg.data.players
            roomInfo.value.status = 1
            boardData.value = Array(15).fill().map(() => Array(15).fill(0))
            winner.value = null
            currentPlayer.value = 1
            winningLine.value = []
            syncIsWatcher()
            showToast('新对局已开始，黑棋先行')
            break
          }
          case 'move':
            boardData.value = msg.data.board
            currentPlayer.value = msg.data.nextPlayer
            break
          case 'result':
            winner.value = msg.data.winner
            if (msg.data.board) {
              boardData.value = msg.data.board
            }
            if (Array.isArray(msg.data.winningLine)) {
              winningLine.value = msg.data.winningLine
            } else {
              winningLine.value = []
            }
            showToast(winnerText.value)
            break
          case 'restart':
          case 'restart_request':
            // 观战端不弹出再来一局对话框
            break
          case 'undo':
            boardData.value = msg.data.board
            currentPlayer.value = msg.data.nextPlayer
            break
          case 'leave':
            showToast(`玩家 ${msg.data.nickname} 已离开房间`)
            players.value = players.value.filter(p => p.userId !== msg.data.userId)
            break
          case 'error':
            showToast(typeof msg.data === 'string' ? msg.data : (msg.data && msg.data.message) || '发生未知错误')
            break
          case 'kick':
            showToast(msg.msg || '您的账号已在其他设备登录')
            if (socket.value) {
              socket.value.close()
            }
            return
          case 'watch':
            isWatcher.value = true
            showToast(msg.msg || '您已切换为观战模式')
            break
          case 'restore':
            if (msg.data) {
              boardData.value = msg.data.board || Array(15).fill().map(() => Array(15).fill(0))
              currentPlayer.value = msg.data.nextPlayer || 1
              players.value = msg.data.players || []
              winner.value = msg.data.winner || null
              if (Array.isArray(msg.data.winningLine)) {
                winningLine.value = msg.data.winningLine
              } else {
                winningLine.value = []
              }
              syncIsWatcher()
              if (!user.userId && userId.value) {
                user.userId = userId.value
              }
              const hasMove = msg.data.board && msg.data.board.some(row => row.some(cell => cell !== 0))
              if (hasMove) {
                showToast('棋局已恢复')
              }
            }
            break
          default:
            showToast('发生未知错误')
            break
        }
      } catch (err) {
        showToast('消息解析失败')
      }
    }
    const connectWebSocket = () => {
      connectWs(socket, wsStatus, {
        userId: userId.value,
        roomId: roomId.value,
        onMessage: onWSMessage
      })
      const trySendRestoreAndJoin = () => {
        setTimeout(() => {
          if (socket.value && socket.value.readyState === WebSocket.OPEN) {
            sendWs(socket, JSON.stringify({
              type: 'restore_request',
              data: { userId: userId.value, roomId: roomId.value }
            }))
            setTimeout(() => {
              if (socket.value && socket.value.readyState === WebSocket.OPEN) {
                sendWs(socket, JSON.stringify({
                  type: 'watchJoin',
                  data: { userId: userId.value, username: user.nickname, roomId: roomId.value }
                }))
              }
            }, 200)
          } else {
            trySendRestoreAndJoin()
          }
        }, 200)
      }
      trySendRestoreAndJoin()
    }
    // 观战端禁止落子
    const placePiece = () => {}
    // 观战端禁止悔棋
    const sendUndo = () => {}
    const isWinningPiece = (x, y) => {
      return Array.isArray(winningLine.value) && winningLine.value.some(([wx, wy]) => wx === x && wy === y)
    }
    onMounted(() => {
      connectWebSocket()
    })
    onBeforeUnmount(() => {
      if (socket.value) closeWs(socket)
    })
    return {
      boardData,
      currentPlayer,
      socket,
      wsStatus,
      winner,
      winningLine,
      players,
      toastMsg,
      roomId,
      user,
      roomInfo,
      leaveLoading,
      isWatcher,
      myPieceText,
      myPieceClass,
      isMyTurn,
      playerCount,
      winnerText,
      indicatorText,
      showLeaveConfirm,
      onLeaveClick,
      confirmLeaveRoom,
      placePiece,
      sendUndo,
      isWinningPiece
    }
  }
}
</script>

<style scoped>
.gobang-container {
  max-width: 600px;
  margin: 40px auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  padding-bottom: 32px;
}
.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 32px 10px 32px;
  font-size: 18px;
  border-bottom: 1px solid #eee;
  background: #f7f7f7;
  border-radius: 12px 12px 0 0;
}
.room-header b {
  font-weight: bold;
  color: #2d8cf0;
}
.black-piece-label,
.white-piece-label {
  color: #222;
  font-weight: bold;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  padding: 0;
  vertical-align: middle;
}
.black-piece-label::before {
  content: '';
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #000;
}
.white-piece-label::before {
  content: '';
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #555;
  box-sizing: border-box;
}
.spectator {
  color: #888;
}
.exit-btn {
  margin-left: auto;
  background: #f56c6c;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 20px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.2s;
  font-weight: bold;
}
.exit-btn:hover {
  background: #d9534f;
}
.exit-btn:disabled {
  background: #bbb;
  cursor: not-allowed;
}
.board-section {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.turn-indicator {
  font-size: 20px;
  margin-bottom: 12px;
  color: #888;
  font-weight: bold;
  transition: color 0.2s;
}
.turn-indicator.active {
  color: #2d8cf0;
}
.board {
  display: flex;
  flex-direction: column;
  border: 2px solid #333;
  background: #dcb35c;
}
.row {
  display: flex;
}
.cell {
  width: 30px;
  height: 30px;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  position: relative;
  background:
      linear-gradient(to right, black, black) center / 100% 1px no-repeat,
      linear-gradient(to bottom, black, black) center / 1px 100% no-repeat;
}
.row:first-child .cell {
  background:
      linear-gradient(to right, black, black) center / 100% 1px no-repeat,
      linear-gradient(to bottom, black, black) bottom / 1px 50% no-repeat;
}
.row:last-child .cell {
  background:
      linear-gradient(to right, black, black) center / 100% 1px no-repeat,
      linear-gradient(to bottom, black, black) top / 1px 50% no-repeat;
}
.cell:first-child {
  background:
      linear-gradient(to right, black, black) right / 50% 1px no-repeat,
      linear-gradient(to bottom, black, black) center / 1px 100% no-repeat;
}
.cell:last-child {
  background:
      linear-gradient(to right, black, black) left / 50% 1px no-repeat,
      linear-gradient(to bottom, black, black) center / 1px 100% no-repeat;
}
.row:first-child .cell:first-child {
  background:
      linear-gradient(to right, black, black) right / 50% 1px no-repeat,
      linear-gradient(to bottom, black, black) bottom / 1px 50% no-repeat;
}
.row:first-child .cell:last-child {
  background:
      linear-gradient(to right, black, black) left / 50% 1px no-repeat,
      linear-gradient(to bottom, black, black) bottom / 1px 50% no-repeat;
}
.row:last-child .cell:first-child {
  background:
      linear-gradient(to right, black, black) right / 50% 1px no-repeat,
      linear-gradient(to bottom, black, black) top / 1px 50% no-repeat;
}
.row:last-child .cell:last-child {
  background:
      linear-gradient(to right, black, black) left / 50% 1px no-repeat,
      linear-gradient(to bottom, black, black) top / 1px 50% no-repeat;
}
.piece {
  width: 90%;
  height: 90%;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
  box-sizing: border-box;
}
.black-piece {
  background: radial-gradient(circle at 30% 30%, #666, #000 80%);
}
.white-piece {
  background: radial-gradient(circle at 70% 70%, #fff, #ddd 80%);
}
.piece.highlight {
  box-shadow: 0 0 12px 4px #ffeb3b;
  border: 2px solid #fff;
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
.watcher-banner.watcher-indicator {
  color: #f56c6c;
  background: #fff0f0;
  border: 1px solid #f56c6c;
  border-radius: 6px;
  padding: 8px 0;
  margin-bottom: 10px;
  font-weight: bold;
  text-align: center;
  font-size: 18px;
}
.restart-dialog-mask {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}
.restart-dialog {
  background: #fff;
  padding: 32px 36px;
  border-radius: 12px;
  min-width: 320px;
  box-shadow: 0 2px 16px rgba(45,140,240,0.08);
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-items: center;
}
.restart-title {
  font-size: 20px;
  color: #2d8cf0;
  font-weight: bold;
}
.restart-actions {
  display: flex;
  gap: 24px;
}
.restart-actions button {
  padding: 10px 32px;
  font-size: 18px;
  background: #2d8cf0;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}
.restart-actions button:disabled {
  background: #bbb;
  cursor: not-allowed;
}
</style>
