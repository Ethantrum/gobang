import { API_URLS } from '@/config/apiConfig';

/**
 * 获取房间列表（分页）
 * @param {number} pageNum 页码（从1开始）
 * @param {number} pageSize 每页条数
 * @returns Promise<{code, msg, data: {total: number, list: Array<Room>}}>
 */
export function fetchRoomList(pageNum = 1, pageSize = 10) {
  const token = localStorage.getItem('token')
  const url = `${API_URLS.ROOM_LIST}?pageNum=${pageNum}&pageSize=${pageSize}`
  
  const headers = {};
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  return fetch(url, {
    method: 'GET',
    headers: headers
  }).then(res => res.json())
}

/**
 * 创建房间
 * @returns Promise<{code, msg, data}>
 */
export function createRoom() {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  return fetch(API_URLS.ROOM_CREATE, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({ userId })
  }).then(res => res.json())
}

/**
 * 加入房间
 * @param {string} roomId
 * @returns Promise<{code, msg, data}>
 */
export function joinRoomById(roomId) {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  return fetch(API_URLS.ROOM_JOIN, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({ userId, roomId })
  }).then(res => res.json())
}

/**
 * 搜索房间（如后端未支持关键词搜索，则前端本地过滤）
 * @param {string} keyword
 * @returns Promise<Array<Room>>
 */
export function searchRoom(keyword) {
  const token = localStorage.getItem('token')
  const url = `${API_URLS.ROOM_SEARCH}?keyword=${encodeURIComponent(keyword)}`
  
  const headers = {};
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  return fetch(url, {
    method: 'GET',
    headers: headers
  }).then(res => res.json())
}

export function leaveRoom(roomId) {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  return fetch(API_URLS.ROOM_LEAVE, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({ userId, roomId })
  }).then(res => res.json())
}

export function roomWatch(roomId) {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  return fetch(API_URLS.ROOM_WATCH, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({ userId, roomId })
  }).then(res => res.json())
}