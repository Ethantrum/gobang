<template>
    <div class="gobang-container">
      <!-- 顶部房间信息区 -->
      <header class="room-header">
        <span>房间号: <b>{{ roomId }}</b></span>
        <span>你的身份: <b :class="myPieceClass">{{ myPieceText }}</b></span>
        <span>当前用户: {{ user.nickname }}</span>
        <button class="exit-btn" @click="handleLeaveRoom" :disabled="leaveLoading">{{ leaveLoading ? '退出中...' : '退出房间' }}</button>
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
        <!-- 胜负横幅与再来一局 -->
        <div v-if="winner" class="result-banner">
          <span>{{ winnerText }}</span>
          <button v-if="!isWatcher" class="restart-btn" @click="sendRestart" :disabled="restartLoading">{{ restartLoading ? '重开中...' : '再来一局' }}</button>
        </div>
      </main>
      <!-- Toast提示挂载点 -->
      <div v-if="toastMsg" class="toast-message">{{ toastMsg }}</div>
      <div v-if="showRestartDialog" class="restart-dialog-mask">
        <div class="restart-dialog">
          <div class="restart-title">对方请求再来一局，是否同意？</div>
          <div class="restart-actions">
            <button @click="agreeRestart" :disabled="restartResponding">同意</button>
            <button @click="rejectRestart" :disabled="restartResponding">拒绝</button>
          </div>
          <div class="restart-tip">{{ restartCountdown }} 秒内未操作将自动拒绝并离开房间</div>
        </div>
      </div>
      <div v-if="waitingRestart" class="restart-waiting">已请求再来一局，等待对方同意…（{{ waitingRestartCountdown }} 秒）</div>
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
    name: 'GobangGame',
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
      const lastOptimisticMove = ref(null)
      const toastMsg = ref('')
      const roomId = ref(route.params.roomId || sessionStorage.getItem('roomId') || 'null')
      const user = globalState.user
      const roomInfo = ref({ status: 0, count: 0 })
      const userId = ref(localStorage.getItem('userId') || 'user_' + Math.floor(Math.random() * 10000))
      const leaveLoading = ref(false)
      const restartLoading = ref(false)
      const showRestartDialog = ref(false)
      const restartResponding = ref(false)
      const waitingRestart = ref(false)
      const restartCountdown = ref(10)
      const isWatcher = ref(false)
      const routeRole = ref(route.query.role || route.params.role || '')
      let restartTimer = null
      let restartInterval = null
      let restartFromUserId = null
      // 发起方等待对方同意的倒计时
      const waitingRestartCountdown = ref(10)
      let waitingRestartTimer = null
      let waitingRestartInterval = null
  
      // 棋子身份文本与样式
      const myPieceText = computed(() => {
        const me = players.value.find(p => p.userId == user.userId)
        if (!me) return routeRole.value === 'watch' ? '旁观者' : '玩家'
        if (me.isWatcher) return '观战'
        if (me.isBlack) return '黑棋'
        if (me.isWhite) return '白棋'
        return '玩家'
      })
      const myPieceClass = computed(() => {
        const me = players.value.find(p => p.userId == user.userId)
        if (!me) return 'spectator'
        if (me.isWatcher) return 'watcher-label'
        if (me.isBlack) return 'black-piece-label'
        if (me.isWhite) return 'white-piece-label'
        return ''
      })
      // 当前回合是否自己
      const isMyTurn = computed(() => {
        const me = players.value.find(p => p.userId == user.userId)
        if (!me || me.isWatcher) return false
        if (me.isBlack && currentPlayer.value === 1) return true
        if (me.isWhite && currentPlayer.value === 2) return true
        return false
      })
      // 非观战玩家数量
      const playerCount = computed(() => players.value.filter(p => !p.isWatcher).length)
      // 胜负横幅文本
      const winnerText = computed(() => {
        if (!winner.value) return ''
        const currentUserId = user.userId
        const me = players.value.find(p => p.userId == currentUserId)
        if (me && me.isWatcher) {
          const winPlayer = players.value.find(p => p.userId == winner.value)
          if (winPlayer) {
            return `游戏结束，${winPlayer.nickname} 获胜！`
          }
          return '对局结束'
        }
        if (winner.value == currentUserId) {
          return '游戏结束，你获得胜利！'
        }
        const winPlayer = players.value.find(p => p.userId == winner.value)
        if (winPlayer) {
          return `游戏结束，${winPlayer.nickname} 获胜！`
        }
        return '对局结束'
      })
  
      // players变化时自动同步isWatcher
      function syncIsWatcher() {
        const me = players.value.find(p => p.userId == user.userId)
        // 优先根据players数据判断，否则用路由参数
        if (me) {
          isWatcher.value = !!me.isWatcher
        } else {
          isWatcher.value = (routeRole.value === 'watch')
        }
      }
  
      // 核心修复：独立的上方状态提示
      const indicatorText = computed(() => {
        if (isWatcher.value) return '观战中：您当前仅可观战，无法操作棋盘'
        if (players.value.length < 2) return '等待玩家加入...'
        if (winner.value) return '游戏结束'
        return isMyTurn.value ? '轮到你下棋' : '等待对方落子'
      })
  
      // Toast 非阻塞提示
      function showToast(msg) {
        toastMsg.value = msg
        setTimeout(() => { toastMsg.value = '' }, 2000)
      }
  
      // 退出房间逻辑
      const handleLeaveRoom = async () => {
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
          switch (msg.type) {
            case 'join':
              players.value = msg.data.players
              // 自动同步 isWatcher
              syncIsWatcher()
              break
            case 'start': {
              players.value = msg.data.players
              roomInfo.value.status = 1
              boardData.value = Array(15).fill().map(() => Array(15).fill(0))
              winner.value = null
              currentPlayer.value = 1
              winningLine.value = []
              // 自动同步 isWatcher
              syncIsWatcher()
              // 优化提示：明确告知用户身份
              const me = msg.data.players.find(p => p.userId == user.userId)
              if (me) {
                showToast(`游戏开始！黑棋先行，你执${me.isBlack ? '黑棋' : '白棋'}。`)
              } else {
                showToast('游戏开始！黑棋先行。')
              }
              waitingRestart.value = false
              if (waitingRestartTimer) clearTimeout(waitingRestartTimer)
              if (waitingRestartInterval) clearInterval(waitingRestartInterval)
              waitingRestartCountdown.value = 10
              break
            }
            case 'move':
              boardData.value = msg.data.board
              currentPlayer.value = msg.data.nextPlayer
              lastOptimisticMove.value = null
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
              
              // 核心修复：游戏结束后，重置所有"再来一局"相关的状态
              if (restartTimer) clearTimeout(restartTimer)
              if (restartInterval) clearInterval(restartInterval)
              showRestartDialog.value = false
              waitingRestart.value = false
              restartResponding.value = false
              restartCountdown.value = 10
              
              break
            case 'restart':
              showRestartDialog.value = true
              restartResponding.value = false
              restartFromUserId = msg.data.fromUserId
              restartCountdown.value = 10
              if (restartTimer) clearTimeout(restartTimer)
              if (restartInterval) clearInterval(restartInterval)
              restartInterval = setInterval(() => {
                restartCountdown.value--
                if (restartCountdown.value <= 0) {
                  clearInterval(restartInterval)
                }
              }, 1000)
              restartTimer = setTimeout(() => {
                if (showRestartDialog.value) {
                  rejectRestart()
                }
              }, 10000)
              waitingRestart.value = false
              break
            case 'restart_request':
              showRestartDialog.value = true
              restartResponding.value = false
              restartFromUserId = msg.data.fromUserId
              restartCountdown.value = 10
              if (restartTimer) clearTimeout(restartTimer)
              if (restartInterval) clearInterval(restartInterval)
              restartInterval = setInterval(() => {
                restartCountdown.value--
                if (restartCountdown.value <= 0) {
                  clearInterval(restartInterval)
                }
              }, 1000)
              restartTimer = setTimeout(() => {
                if (showRestartDialog.value) {
                  rejectRestart()
                }
              }, 10000)
              waitingRestart.value = false
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
              // 处理后端主动推送的错误消息
              if (typeof msg.data === 'string' && msg.data.includes('玩家不足')) {
                showToast('房间内玩家不足，无法开始新对局。')
                waitingRestart.value = false
                restartLoading.value = false
                break
              }
              showToast(typeof msg.data === 'string' ? msg.data : (msg.data && msg.data.message) || '发生未知错误')
              if (lastOptimisticMove.value) {
                const { x, y } = lastOptimisticMove.value
                boardData.value[x][y] = 0
                lastOptimisticMove.value = null
              }
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
            case 'master':
              isWatcher.value = false
              showToast(msg.msg || '您已成为主控端，可以操作棋盘')
              break
            case 'restart_response':
              if (msg.data === true) {
                showToast('对方同意再来一局，正在开始新对局…')
                waitingRestart.value = false
                // 不再自动发restart，由后端统一推送start
              } else {
                showToast('对方拒绝了再来一局请求')
                waitingRestart.value = false
              }
              break
            default:
              if (typeof msg.data === 'string' && msg.data.includes('拒绝再来一局')) {
                showToast(msg.data)
                waitingRestart.value = false
              } else if (msg.data && msg.data.message && msg.data.message.includes('拒绝再来一局')) {
                showToast(msg.data.message)
                waitingRestart.value = false
              } else {
                showToast('发生未知错误')
              }
              break
          }
        } catch (err) {
          showToast('消息解析失败')
        }
      }
  
      // 连接WebSocket
      const connectWebSocket = () => connectWs(socket, wsStatus, {
        userId: userId.value,
        roomId: roomId.value,
        onMessage: onWSMessage
      })
  
      // 发送加入房间/观战
      const sendJoin = () => {
        if (socket.value) {
          if (routeRole.value === 'watch') {
            sendWs(socket, JSON.stringify({
              type: 'watchJoin',
              data: { userId: userId.value, username: user.nickname, roomId: roomId.value }
            }))
          } else {
            sendWs(socket, JSON.stringify({
              type: 'join',
              data: { userId: userId.value, username: user.nickname, roomId: roomId.value }
            }))
          }
        }
      }
  
      // 落子
      const placePiece = (x, y) => {
        const me = players.value.find(p => p.userId == user.userId)
        if (me && me.isWatcher) return // 观战者不能下棋
        if (boardData.value[x][y] !== 0 || winner.value) return
        // 乐观UI更新
        boardData.value[x][y] = currentPlayer.value
        lastOptimisticMove.value = { x, y }
        if (socket.value) {
          sendWs(socket, JSON.stringify({
            type: 'move',
            data: { roomId: roomId.value, x, y }
          }))
        }
      }
  
      // 悔棋
      const sendUndo = () => {
        if (isWatcher.value) return
        if (socket.value) sendWs(socket, JSON.stringify({ type: 'undo' }))
      }
  
      // 重开
      const sendRestart = async () => {
        if (isWatcher.value) return
        // 新增：房间玩家不足2人时提示
        const playerCount = players.value.filter(p => !p.isWatcher).length
        if (playerCount < 2) {
          showToast('房间内玩家不足，无法开始新对局。')
          return
        }
        if (socket.value) {
          waitingRestart.value = true
          waitingRestartCountdown.value = 10
          if (waitingRestartTimer) clearTimeout(waitingRestartTimer)
          if (waitingRestartInterval) clearInterval(waitingRestartInterval)
          waitingRestartInterval = setInterval(() => {
            waitingRestartCountdown.value--
            if (waitingRestartCountdown.value <= 0) {
              clearInterval(waitingRestartInterval)
            }
          }, 1000)
          waitingRestartTimer = setTimeout(() => {
            waitingRestart.value = false
            waitingRestartCountdown.value = 10
          }, 10000)
          sendWs(socket, JSON.stringify({ type: 'restart_request', data: { roomId: roomId.value, userId: user.userId } }))
        }
      }
  
      // 同意再来一局
      const agreeRestart = () => {
        if (isWatcher.value) return
        restartResponding.value = true
        showRestartDialog.value = false
        if (restartTimer) clearTimeout(restartTimer)
        if (restartInterval) clearInterval(restartInterval)
        if (socket.value) {
          sendWs(socket, JSON.stringify({ type: 'restart_response', data: { agree: true, fromUserId: restartFromUserId, roomId: roomId.value } }))
        }
      }
  
      // 拒绝再来一局
      const rejectRestart = () => {
        if (isWatcher.value) return
        restartResponding.value = true
        showRestartDialog.value = false
        if (restartTimer) clearTimeout(restartTimer)
        if (restartInterval) clearInterval(restartInterval)
        if (socket.value) {
          sendWs(socket, JSON.stringify({ type: 'restart_response', data: { agree: false, fromUserId: restartFromUserId, roomId: roomId.value } }))
        }
        // 自动离开房间
        setTimeout(() => {
          handleLeaveRoom()
        }, 500)
      }
  
      // 退出
      const sendLeave = () => {
        if (socket.value) sendWs(socket, JSON.stringify({ type: 'leave' }))
      }
  
      // 新增：判断是否为高亮棋子
      const isWinningPiece = (x, y) => {
        return winningLine.value.some(p => p.x === x && p.y === y);
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
        if (restartTimer) clearTimeout(restartTimer)
        if (restartInterval) clearInterval(restartInterval)
        if (waitingRestartTimer) clearTimeout(waitingRestartTimer)
        if (waitingRestartInterval) clearInterval(waitingRestartInterval)
        sendLeave()
        closeWs(socket)
      })
  
      return {
        boardData, currentPlayer, placePiece, winner, players,
        sendUndo, sendRestart, sendLeave, roomId, user, roomInfo,
        handleLeaveRoom, lastOptimisticMove, myPieceText, myPieceClass, isMyTurn, winnerText, toastMsg,
        leaveLoading, restartLoading,
        showRestartDialog,
        agreeRestart,
        rejectRestart,
        restartResponding,
        waitingRestart,
        restartCountdown,
        indicatorText,
        isWinningPiece,
        isWatcher,
        waitingRestartCountdown,
        playerCount
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