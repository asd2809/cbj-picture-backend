package com.yupi.yupictureDDD.shared.websocket.disruptor;


import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import javax.annotation.Resource;

/**
 * 图片编辑Disruptor配置
 * 将刚定义的事件及处理器关联到Disruptor实例中
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    /// 注入事件处理器
    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    /// 创建并注册 Disruptor 实例到 Spring 容器 的过程
    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        // 定义ringbuffer的大小
        int bufferSize = 1024 * 256;
        // 创建disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,   ///告诉环形缓冲区的槽位里放的对象类型是PictureEditEvent
                bufferSize,              ///自己定义环形队列的大小
                ThreadFactoryBuilder.create() ///固定方法
                        .setNamePrefix("pictureEditEventDisruptor")///线程前缀
                        .build()   /// 创建线程工厂
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler); ///必须注册消费者，但形式可以不同
        // 启动disruptor
        disruptor.start();  /// 必须写
        return disruptor;
    }

}
