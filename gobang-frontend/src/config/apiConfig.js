// 五子棋项目API配置

export const API_BASE = 'http://192.168.0.187:8081';
export const WS_BASE = 'ws://192.168.0.187:8081';

export const API_URLS = {
  // 用户相关
  USER_REGISTER: API_BASE + '/api/user/register', // POST {username, nickname, password, email}
  USER_LOGIN: API_BASE + '/api/user/login',       // POST {username, password}
  USER_INFO: API_BASE + '/api/user/info',         // GET ?userId=xxx

  // 房间相关
  ROOM_CREATE: API_BASE + '/api/room/create',     // POST {userId, username}
  ROOM_JOIN: API_BASE + '/api/room/join',         // POST {userId, roomId}
  ROOM_LIST: API_BASE + '/api/room/list',         // GET
  ROOM_INFO: API_BASE + '/api/room/info',         // GET ?roomId=xxx
  ROOM_CURRENT: API_BASE + '/api/room/current-room', // GET
  ROOM_LEAVE: API_BASE + '/api/room/leave',       // POST
  ROOM_SEARCH: API_BASE + '/api/room/search',     // GET

  // 游戏相关（REST 辅助）
  GAME_BOARD: API_BASE + '/api/game/board',       // GET ?roomId=xxx
  GAME_RESTART: API_BASE + '/api/game/restart',   // POST {roomId}
  GAME_UNDO: API_BASE + '/api/game/undo',         // POST {roomId}

  // WebSocket 实时对战
  GAME_GOBANG: WS_BASE + '/ws/game',              // ws://localhost:8081/ws/game?userId=xxx&roomId=xxx
};