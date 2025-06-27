<template>
  <div class="gobang-container">
    <header class="room-header">
      <span>房间号: <b>{{ roomId }}</b></span>
      <span>你的身份: <b :class="myPieceClass">{{ myPieceText }}</b></span>
      <span>当前用户: {{ user.nickname }}</span>
      <button class="exit-btn" @click="onLeaveClick" :disabled="leaveLoading">{{ leaveLoading ? '退出中...' : '退出房间' }}</button>
    </header>
    <main class="board-section">
      <div class="turn-indicator" :class="{ active: isMyTurn && playerCount === 2 && !winner }">
        {{ indicatorText }}
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
      <div v-if="winner" class="result-banner">
        <span>{{ winnerText }}</span>
        <button class="restart-btn" @click="sendRestart" :disabled="restartLoading">{{ restartLoading ? '重开中...' : '再来一局' }}</button>
      </div>
    </main>
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
import { globalState } from '@/globalState'
import { ref } from 'vue'

export default {
  name: 'GobangPlayer',
  setup() {
    const user = globalState.user
    // 棋盘初始化为15x15的二维数组，初始全为0
    const boardData = ref(Array.from({ length: 15 }, () => Array(15).fill(0)))
    // ...原有setup逻辑...
    return {
      user,
      boardData,
      // ...其余返回项...
    }
  }
}
</script> 