package com.example.gobang.server.handler;

import com.alibaba.fastjson.JSON;
import com.example.gobang.common.result.WSResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
@Controller
public class MyWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WSDispatcher dispatcher;

    @Autowired
    private WSSessionManager wsSessionManager;

    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket连接建立: " + session.getId());
        wsSessionManager.add(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            System.out.println("收到消息: " + message.getPayload());
            dispatcher.dispatch(session, message);
        } catch (Exception e) {
            System.err.println("处理消息时发生异常: " + e.getMessage());
            e.printStackTrace();
            
            // 异常时仍然只通知当前用户
            WSResult<String> errorResult = WSResult.error("消息处理失败: " + e.getMessage());
            String errorJson = JSON.toJSONString(errorResult);
            session.sendMessage(new TextMessage(errorJson));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket连接关闭: " + session.getId() + ", 状态: " + status);
        // 如果session中存了roomId和userId，说明用户是在房间中断开的，需要触发leave逻辑来清理数据和通知对手
        if (session.getAttributes().containsKey("roomId")) {
            dispatcher.dispatch(session, new TextMessage("{\"type\":\"leave\"}"));
        }
        wsSessionManager.remove(session);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
        exception.printStackTrace();
    }
}
