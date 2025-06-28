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
      <div class="board">
        <div v-for="(row, rowIndex) in 15" :key="rowIndex" class="row">
          <div v-for="(col, colIndex) in 15" :key="colIndex" class="cell">
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
      <!-- 胜负横幅与再来一局 -->
      <div v-if="winner" class="result-banner">
        <span>{{ winnerText }}</span>
      </div>
    </main>
    <!-- Toast提示挂载点 -->
    <div v-if="toastMsg" class="toast-message">{{ toastMsg }}</div>
    <!-- 退出房间二次确认弹窗 -->
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
import { fetchUserInfo } from '@/services/userService'
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
    // 保证user.userId有值
    if (!user.userId && userId.value) {
      user.userId = userId.value
    }
    const leaveLoading = ref(false)
    const isWatcher = ref(true) // 观战页面固定为观战者
    const showLeaveConfirm = ref(false)

    // 棋子身份文本与样式
    const myPieceText = computed(() => {
      return '观战者'
    })
    const myPieceClass = computed(() => {
      return 'watcher-label'
    })
    
    // 胜负横幅文本
    const winnerText = computed(() => {
      if (!winner.value) return ''
      const winPlayer = players.value.find(p => p.userId == winner.value)
      if (winPlayer) {
        return `游戏结束，${winPlayer.nickname} 获胜！`
      }
      return '对局结束'
    })

    // 观战状态提示
    const indicatorText = computed(() => {
      if (players.value.length < 2) return '等待玩家加入...'
      if (winner.value) return '游戏结束'
      return '观战中：您当前仅可观战，无法操作棋盘'
    })

    // players变化时自动同步isWatcher
    function syncIsWatcher() {
      const me = players.value.find(p => p.userId == user.userId)
      // 优先根据players数据判断，否则用路由参数
      if (me) {
        isWatcher.value = !!me.isWatcher
      } else {
        isWatcher.value = (route.query.role === 'watch')
      }
    }

    // Toast 非阻塞提示
    function showToast(msg) {
      toastMsg.value = msg
      setTimeout(() => { toastMsg.value = '' }, 2000)
    }

    // 退出房间逻辑
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

    // 获取房间信息
    const fetchRoomInfo = async () => {
      try {
        // 这里可以调用获取房间详情的接口
        // const res = await fetchRoomInfo(roomId.value)
        // if (res.code === 0) {
        //   roomInfo.value = res.data
        // }
      } catch (e) {
        showToast('获取房间信息失败')
      }
    }

    // 处理服务端消息
    const onWSMessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        console.log('[WS] onWSMessage', msg)
        switch (msg.type) {
          case 'join':
            players.value = msg.data.players
            // 自动同步 isWatcher
            syncIsWatcher()
            break
          case 'start': {
            console.log('[WS][start] 收到start消息', msg)
            players.value = msg.data.players
            roomInfo.value.status = 1
            boardData.value = Array(15).fill().map(() => Array(15).fill(0))
            winner.value = null
            currentPlayer.value = 1
            winningLine.value = []
            // 自动同步 isWatcher
            syncIsWatcher()
            // 观战者也刷新棋盘和玩家信息
            if (isWatcher.value) {
              showToast('新对局已开始，黑棋先行')
            } else {
              // 优化提示：明确告知用户身份
              const me = msg.data.players.find(p => p.userId == user.userId)
              if (me && !me.isWatcher) {
                showToast(`游戏开始！黑棋先行，你执${me.isBlack ? '黑棋' : '白棋'}。`)
              } else {
                showToast('游戏开始！黑棋先行。')
              }
            }
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
            if (msg.data.winningLine) {
              winningLine.value = msg.data.winningLine
            }
            showToast(winnerText.value)
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
          case 'validation_error':
            // 验证错误（如位置已有棋子、无效坐标等）
            showToast(msg.data || '验证失败')
            break
          case 'permission_error':
            // 权限错误（如身份验证失败、操作权限不足等）
            showToast(msg.data || '权限不足')
            break
          case 'turn_error':
            // 回合错误（如不是自己的回合）
            showToast(msg.data || '不是您的回合')
            break
          case 'game_state_error':
            // 游戏状态错误（如房间玩家不足、对局不存在等）
            showToast(msg.data || '游戏状态错误')
            break
          case 'connection_error':
            // 连接错误（如连接参数错误、非法连接等）
            showToast(msg.data || '连接错误')
            if (socket.value) {
              socket.value.close()
            }
            setTimeout(() => {
              router.push({ name: 'GameHall' })
            }, 1000)
            break
          case 'system_error':
            // 系统错误（如消息处理失败等）
            showToast(msg.data || '系统错误')
            break
          case 'kick':
            showToast(msg.data || '您已被踢出房间')
            if (socket.value) {
              socket.value.close()
            }
            setTimeout(() => {
              router.push({ name: 'GameHall' })
            }, 1000)
            break
          case 'watch':
            isWatcher.value = true
            showToast(msg.msg || '您已切换为观战模式')
            break
          case 'master':
            isWatcher.value = false
            showToast(msg.msg || '您已成为主控端，可以操作棋盘')
            break
          case 'restore':
            // 新增：断线重连恢复棋局
            if (msg.data) {
              boardData.value = msg.data.board || Array(15).fill().map(() => Array(15).fill(0))
              currentPlayer.value = msg.data.nextPlayer || 1
              players.value = msg.data.players || []
              winner.value = msg.data.winner || null
              winningLine.value = msg.data.winningLine || []
              syncIsWatcher()
              // 保证user.userId有值
              if (!user.userId && userId.value) {
                user.userId = userId.value
              }
              // 只有有落子时才提示
              const hasMove = msg.data.board && msg.data.board.some(row => row.some(cell => cell !== 0))
              // 日志：重连恢复后关键信息
              console.log('[WS][restore] players:', players.value)
              console.log('[WS][restore] userId:', user.userId)
              const me = players.value.find(p => p.userId == user.userId)
              console.log('[WS][restore] me:', me)
              console.log('[WS][restore] myPieceText:', myPieceText.value)
              console.log('[WS][restore] myPieceClass:', myPieceClass.value)
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

    // 连接WebSocket
    const connectWebSocket = () => {
      console.log('[WS] connectWebSocket called');
      connectWs(socket, wsStatus, {
        userId: userId.value,
        roomId: roomId.value,
        onMessage: onWSMessage
      })
      // 先恢复棋局再自动join
      const trySendRestoreAndJoin = () => {
        setTimeout(() => {
          if (socket.value && socket.value.readyState === WebSocket.OPEN) {
            // 1. 先恢复棋局
            console.log('[WS] send watchRestore', { userId: userId.value, roomId: roomId.value });
            sendWs(socket, JSON.stringify({
              type: 'watchRestore',
              data: { userId: userId.value, roomId: roomId.value }
            }))
            // 2. 再join房间，确保后端注册session
            setTimeout(() => {
              if (socket.value && socket.value.readyState === WebSocket.OPEN) {
                console.log('[WS] send watchJoin', { userId: userId.value, username: user.nickname, roomId: roomId.value });
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

    // 发送加入房间/观战
    const sendJoin = () => {
      if (socket.value) {
        sendWs(socket, JSON.stringify({
          type: 'watchJoin',
          data: { userId: userId.value, username: user.nickname, roomId: roomId.value }
        }))
      }
    }

    // 悔棋
    const sendUndo = () => {
      if (isWatcher.value) return
      if (socket.value) sendWs(socket, JSON.stringify({ type: 'undo' }))
    }

    // 退出
    const sendLeave = () => {
      if (socket.value) sendWs(socket, JSON.stringify({ type: 'leave' }))
    }

    // 新增：判断是否为高亮棋子
    const isWinningPiece = (x, y) => {
      return winningLine.value.some(p => p[0] === x && p[1] === y);
    };

    onMounted(async () => {
      // 先拉取用户信息
      try {
        await fetchUserInfo()
        await fetchRoomInfo()
      } catch (e) {
        // eslint-disable-next-line no-empty
      }
      connectWebSocket()
      setTimeout(sendJoin, 500)
    })
    onBeforeUnmount(() => {
      sendLeave()
      closeWs(socket)
    })

    return {
      boardData, currentPlayer, winner, players,
      sendUndo, sendLeave, roomId, user, roomInfo,
      myPieceText, myPieceClass, winnerText, toastMsg,
      leaveLoading,
      indicatorText,
      isWinningPiece,
      isWatcher,
      showLeaveConfirm,
      onLeaveClick,
      confirmLeaveRoom
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
.board-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 24px;
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
  border: 1.5px solid #bbb;
}
.result-banner {
  margin-top: 18px;
  background: #2d8cf0;
  color: #fff;
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 20px;
  display: flex;
  align-items: center;
  gap: 24px;
  box-shadow: 0 2px 8px rgba(45,140,240,0.08);
}
.restart-btn {
  background: #4CAF50;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 20px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.2s;
  font-weight: bold;
  margin-left: 18px;
}
.restart-btn:hover {
  background: #388e3c;
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
.restart-tip {
  color: #888;
  font-size: 15px;
}
.restart-waiting {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  background: #2d8cf0;
  color: #fff;
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 18px;
  z-index: 2000;
  box-shadow: 0 2px 8px rgba(45,140,240,0.08);
}
.piece.highlight {
  box-shadow: 0 0 12px 4px #ffeb3b;
  border: 2px solid #fff;
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
</style>