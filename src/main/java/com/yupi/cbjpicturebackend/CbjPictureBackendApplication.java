package com.yupi.cbjpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("com.yupi.cbjpicturebackend.mapper")
//与代理有关
@EnableAspectJAutoProxy(exposeProxy = true)
public class CbjPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CbjPictureBackendApplication.class, args);
    }

}
