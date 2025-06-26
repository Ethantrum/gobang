package com.example.gobang.server.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WSDispatcher 是 WebSocket 消息的分发器。
 * 
 * 作用：
 *   - 启动时扫描业务处理类（如 GameWsService）所有带 @WSMessageHandler 注解的方法，
 *     建立 type 到方法的映射。
 *   - 收到消息后，根据 type 自动找到对应方法，并用反射调用。
 *   - 自动将 data 字段内容转为方法需要的参数类型，并注入。
 *   - 支持方法返回值，handleMessage 可统一处理返回结果。
 * 
 * 反射分发原理：
 *   1. 构造时遍历所有方法，找到 @WSMessageHandler 注解，记录 type -> method 映射。
 *   2. 收到消息时，dispatch(type, session, data)：
 *      - 找到对应 method
 *      - 用 fastjson 把 data 转为 method 需要的参数类型
 *      - 反射调用 method，自动注入参数
 *      - 返回 method 的返回值
 */
@Component
public class WSDispatcher {

    @Autowired
    private Collection<WebSocketMessageHandler> handlers;

    private final Map<String, Method> handlerMap = new HashMap<>();
    private final Map<String, Object> handlerInstanceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (Object handlerInstance : handlers) {
            for (Method method : handlerInstance.getClass().getDeclaredMethods()) {
                WSMessageHandler annotation = method.getAnnotation(WSMessageHandler.class);
                if (annotation != null) {
                    String type = annotation.value();
                    if (handlerMap.containsKey(type)) {
                        throw new IllegalStateException("Duplicate WSMessageHandler for type: " + type);
                    }
                    method.setAccessible(true);
                    handlerMap.put(type, method);
                    handlerInstanceMap.put(type, handlerInstance);
                }
            }
        }
    }

    /**
     * 分发消息，根据 type 自动调用对应业务方法。
     * @param type    消息类型（前端传来的type）
     * @param session 当前 WebSocket 连接会话
     * @param data    前端 data 字段内容（fastjson对象）
     * @return        业务方法的返回值（会作为data字段回写给前端）
     * @throws Exception 反射调用异常
     */
    public void dispatch(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = (String) message.getPayload();
        System.out.println("解析消息: " + payload);

        JSONObject jsonObject = JSON.parseObject(payload);
        String type = jsonObject.getString("type");
        JSONObject data = jsonObject.getJSONObject("data");
        
        System.out.println("消息类型: " + type + ", 数据: " + data);
        
        Method handlerMethod = handlerMap.get(type);
        Object handlerInstance = handlerInstanceMap.get(type);

        if (handlerMethod != null && handlerInstance != null) {
            System.out.println("调用方法: " + handlerMethod.getName() + " 参数: " + data);
            
            // 检查方法的参数类型
            Class<?>[] paramTypes = handlerMethod.getParameterTypes();
            if (paramTypes.length == 2 &&
                paramTypes[0].isAssignableFrom(WebSocketSession.class) &&
                paramTypes[1].isAssignableFrom(JSONObject.class)) {
                handlerMethod.invoke(handlerInstance, session, data);
            } else {
                 System.err.println("方法 " + handlerMethod.getName() + " 的签名不符合 (WebSocketSession, JSONObject) 要求");
            }
            
        } else {
            System.out.println("未找到类型为 '" + type + "' 的处理器");
        }
    }
}
