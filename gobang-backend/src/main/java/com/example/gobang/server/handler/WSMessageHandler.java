package com.example.gobang.server.handler;

import java.lang.annotation.*;
 
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WSMessageHandler {
    String value();
} 