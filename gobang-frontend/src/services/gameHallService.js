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
  return fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': token ? 'Bearer ' + token : ''
    }
  }).then(res => res.json())
}

/**
 * 创建房间
 * @returns Promise<{code, msg, data}>
 */
export function createRoom() {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  return fetch(API_URLS.ROOM_CREATE, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    },
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
  return fetch(API_URLS.ROOM_JOIN, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    },
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
  return fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': token ? 'Bearer ' + token : ''
    }
  }).then(res => res.json())
}

/**
 * 处理创建房间按钮点击事件，跳转到五子棋页面
 * @param {Object} router - Vue Router 实例
 */
export const handleCreateRoom = async (router) => {
    const token = localStorage.getItem('token')
    if (!token) {
        alert('请先登录')
        return
    }

    try {
        const response = await fetch(API_URLS.CREATE_ROOM, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `${token}`
            }
        });

        if (!response.ok) {
            throw new Error(`请求失败: ${response.status}`);
        }

        const data = await response.json();

        console.log('创建房间响应:', data);
        console.log('携带的roomId:', data.data);

        router.push({
            name: 'Gobang',
            params: {
                roomId: data.data // 假设返回数据结构为 { msg: "...", data: "房间ID" }
            }
        });

    } catch (error) {
        alert(`创建房间失败: ${error.message}`);
    }
}

export function leaveRoom(roomId) {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  return fetch(API_URLS.ROOM_LEAVE, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    },
    body: JSON.stringify({ userId, roomId })
  }).then(res => res.json())
}

export function roomWatch(roomId) {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  return fetch(API_URLS.ROOM_WATCH, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    },
    body: JSON.stringify({ userId, roomId })
  }).then(res => res.json())
}