package com.yupi.yupictureDDD;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class}) //关闭分库分表
@MapperScan("com.yupi.yupictureDDD.infrastructure.mapper")
//与代理有关
@EnableAspectJAutoProxy(exposeProxy = true)
public class CbjPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CbjPictureBackendApplication.class, args);
    }

}
