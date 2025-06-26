package com.example.gobang.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class WSResult<T> implements Serializable {

    private String type;
    private T data;

    public static <T> WSResult<T> join(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "join";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> start(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "start";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> move(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "move";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> restore(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "restore";
        wsResult.data = object;
        return wsResult;
    }
    public static <T> WSResult<T> restart(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "restart";
        wsResult.data = object;
        return wsResult;
    }
    public static <T> WSResult<T> undo(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "undo";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> result(T object) {
        WSResult<T> wsResult = new WSResult<>();
        wsResult.type = "result";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> leave(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "leave";
        wsResult.data = object;
        return wsResult;
    }
    public static <T> WSResult<T> error(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "error";
        wsResult.data = object;
        return wsResult;
    }

    public static WSResult<Void> restartRequest() {
        WSResult<Void> wsResult = new WSResult<>();
        wsResult.type = "restart_request";
        wsResult.data = null;
        return wsResult;
    }

    public static WSResult<Boolean> restartResponse(boolean agree) {
        WSResult<Boolean> wsResult = new WSResult<>();
        wsResult.type = "restart_response";
        wsResult.data = agree;
        return wsResult;
    }

    public static WSResult<String> master(String msg) {
        WSResult<String> wsResult = new WSResult<>();
        wsResult.type = "master";
        wsResult.data = msg;
        return wsResult;
    }
}
