// 棋盘相关
export const BOARD_SIZE = 15;

// 角色类型
export const ROLE = {
  PLAYER: 0,
  BLACK: 1,
  WHITE: 2,
  WATCHER: 3,
};

// 消息类型
export const WS_MSG_TYPE = {
  JOIN: 'join',
  START: 'start',
  MOVE: 'move',
  RESTORE: 'restore',
  RESTART: 'restart',
  UNDO: 'undo',
  RESULT: 'result',
  LEAVE: 'leave',
  ERROR: 'error',
  // ...如有其它类型可补充
};

// 表单校验正则
export const REGEX = {
  USERNAME: /^[a-zA-Z0-9_]{4,16}$/,
  NICKNAME: /^[\u4e00-\u9fa5a-zA-Z0-9_]{2,16}$/,
  PASSWORD: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/,
  EMAIL: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
}; 