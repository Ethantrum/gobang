package com.example.gobang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.gobang.server.mapper")
public class GobangApplication {

    public static void main(String[] args) {
        SpringApplication.run(GobangApplication.class, args);
    }

}