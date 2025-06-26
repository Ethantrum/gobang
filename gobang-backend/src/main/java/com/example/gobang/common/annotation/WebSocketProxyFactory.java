package com.example.gobang.common.annotation;

import org.springframework.web.socket.WebSocketHandler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * WebSocket代理工厂，用于创建带消息分发功能的WebSocket处理器
 */
public class WebSocketProxyFactory {
    /**
     * 创建WebSocket处理器代理实例
     * @param target 实际的消息处理目标对象
     * @return WebSocketHandler 代理实例
     */
    public static WebSocketHandler createProxy(Object target) {
        return (WebSocketHandler) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            new Class[]{WebSocketHandler.class},
            new WebSocketMessageDispatcher(target)
        );
    }

    /**
     * 消息分发处理器，实现基本的方法代理
     */
    static class WebSocketMessageDispatcher implements InvocationHandler {
        private final Object target;

        public WebSocketMessageDispatcher(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 调用实际的目标方法
            return method.invoke(target, args);
        }
    }
}
