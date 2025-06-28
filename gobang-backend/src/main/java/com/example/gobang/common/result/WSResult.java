package com.example.gobang.common.result;

import lombok.Data;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

@Data
public class WSResult<T> implements Serializable {

    private String type;
    private T data;

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

    public static <T> WSResult<T> error(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "error";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> validationError(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "validation_error";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> permissionError(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "permission_error";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> kick(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "kick";
        wsResult.data = object;
        return wsResult;
    }


    public static WSResult<JSONObject> restartRequest(Long fromUserId) {
        WSResult<JSONObject> wsResult = new WSResult<>();
        wsResult.type = "restart_request";
        JSONObject obj = new JSONObject();
        obj.put("fromUserId", fromUserId);
        wsResult.data = obj;
        return wsResult;
    }

    public static WSResult<JSONObject> restartResponse(boolean agree, Long fromUserId) {
        WSResult<JSONObject> wsResult = new WSResult<>();
        wsResult.type = "restart_response";
        JSONObject obj = new JSONObject();
        obj.put("agree", agree);
        obj.put("fromUserId", fromUserId);
        wsResult.data = obj;
        return wsResult;
    }


    public static <T> WSResult<T> connectionError(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "connection_error";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> turnError(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "turn_error";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> gameStateError(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "game_state_error";
        wsResult.data = object;
        return wsResult;
    }

    public static <T> WSResult<T> systemError(T object){
        WSResult<T> wsResult = new WSResult<T>();
        wsResult.type = "system_error";
        wsResult.data = object;
        return wsResult;
    }
}
