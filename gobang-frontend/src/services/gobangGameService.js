import { API_URLS } from '@/config/apiConfig';

//获取服务器连接
export const getWs = (params = {}) => {
    // 将参数对象转换为查询字符串
    const queryString = new URLSearchParams(params).toString();
    // 构建完整的WebSocket URL，包含参数
    const url = queryString
        ? `${API_URLS.GAME_GOBANG}?${queryString}`
        : API_URLS.GAME_GOBANG;

    return new WebSocket(url);
};

// WebSocket连接函数
export const connectWs = (socket, wsStatus, { userId = 'user', roomId = '', onMessage } = {}) => {
    if (wsStatus.value === "已断开") {
        socket.value = getWs({ userId, roomId });

        socket.value.onopen = () => handleOpen(wsStatus);
        socket.value.onclose = () => handleClose(wsStatus);
        socket.value.onerror = () => {
            wsStatus.value = '连接失败';
            console.error('WebSocket 连接失败');
        };
        // 允许页面自定义消息处理
        socket.value.onmessage = (e) => {
            if (onMessage) {
                onMessage(e);
            } else {
                handleMessage(e);
            }
        };
    } else {
        console.log("——————连接已建立——————");
    }
};

// 发送消息函数
export const sendWs = (socket, message) => {
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
        socket.value.send(message);
    } else {
        console.error('WebSocket 未处于打开状态，无法发送消息');
    }
};

// 关闭连接函数
export const closeWs = (socket) => {
    if (socket.value) socket.value.close()
}

const handleOpen = (wsStatus) => {
    wsStatus.value = '已连接';
    console.log("——————连接建立——————");
}

const handleClose = (wsStatus) => {
    wsStatus.value = '已断开';
    console.log("——————连接关闭——————");
}

const handleMessage = (e) => {
    console.log('收到消息:', e.data);
}
