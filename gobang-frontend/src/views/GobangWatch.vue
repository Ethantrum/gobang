<template>
  <div class="gobang-container">
    <header class="room-header">
      <span>房间号: <b>{{ roomId }}</b></span>
      <span>你的身份: <b class="watcher-label">观战者</b></span>
      <span>当前用户: {{ user.nickname }}</span>
      <button class="exit-btn" @click="onLeaveClick" :disabled="leaveLoading">{{ leaveLoading ? '退出中...' : '退出房间' }}</button>
    </header>
    <main class="board-section">
      <div class="watcher-banner watcher-indicator">观战中：您当前仅可观战，无法操作棋盘</div>
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
      <div v-if="winner" class="result-banner">
        <span>{{ winnerText }}</span>
      </div>
    </main>
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
// 省略：可复用原GobangGame.vue中观战相关逻辑，去除落子、悔棋、再来一局等分支
</script> 