<template>
  <div class="gobang-board-wrapper">
    <div class="board">
      <div v-for="(row, rowIndex) in 15" :key="rowIndex" class="row">
        <div v-for="(col, colIndex) in 15" :key="colIndex" class="cell" @click="onCellClick(rowIndex, colIndex)">
          <div v-if="boardData[rowIndex][colIndex]"
               :class="[boardData[rowIndex][colIndex] === 1 ? 'black-piece' : 'white-piece', 'piece']">
          </div>
        </div>
      </div>
    </div>
    <div class="board-mask"></div>
  </div>
</template>

<script>
export default {
  name: 'GobangBoard',
  props: {
    boardData: {
      type: Array,
      required: true
    },
    winner: {
      type: [Number, null],
      default: null
    }
  },
  emits: ['cell-click'],
  methods: {
    onCellClick(x, y) {
      if (this.winner) return;
      this.$emit('cell-click', x, y);
    }
  }
}
</script>

<style scoped>
.gobang-board-wrapper {
  position: relative;
  width: 460px;
  height: 460px;
  margin: 0 auto;
}
.board {
  position: absolute;
  left: 0; top: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 2px solid #333;
  background: #dcb35c;
  z-index: 1;
}
.row {
  display: flex;
  flex: 1;
}
.cell {
  flex: 1;
  height: 100%;
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
.piece {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.18);
  margin: 2px;
  transition: transform 0.15s, box-shadow 0.15s;
  animation: popin 0.18s;
}
@keyframes popin {
  0% { transform: scale(0.5); opacity: 0.5; }
  100% { transform: scale(1); opacity: 1; }
}
.black-piece {
  background: radial-gradient(circle at 30% 30%, #666, #000 80%);
}
.white-piece {
  background: radial-gradient(circle at 70% 70%, #fff, #ddd 80%);
  border: 1.5px solid #bbb;
}
.board-mask {
  pointer-events: none;
  position: absolute;
  left: 0; top: 0; right: 0; bottom: 0;
  z-index: 2;
  /* 回字形状遮罩，四角覆盖，中心透明 */
  background:
    linear-gradient(#fff 0 0) top left/40px 40px no-repeat,
    linear-gradient(#fff 0 0) top right/40px 40px no-repeat,
    linear-gradient(#fff 0 0) bottom left/40px 40px no-repeat,
    linear-gradient(#fff 0 0) bottom right/40px 40px no-repeat;
  background-repeat: no-repeat;
}
@media (max-width: 500px) {
  .gobang-board-wrapper {
    width: 98vw;
    height: 98vw;
    min-width: 240px;
    min-height: 240px;
    max-width: 99vw;
    max-height: 99vw;
  }
}
</style> 