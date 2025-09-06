package com.yupi.cbjpicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicture.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 图片编辑生产者
 */

@Component
@Slf4j
public class PictureEditEventProducer {

    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /// 申请槽位 → 写入数据 → 发布事件”
    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer(); ///1.获取ringBuffer（环形缓冲区）必须写
        //获取到可以防止事件的位置
        long next = ringBuffer.next();                                                     ///2.申请可用槽位(返回下一个可用槽位)

        PictureEditEvent pictureEditEvent = ringBuffer.get(next);                         /// 引用了disruptor已经存在的事件对象(填充的是从环形槽位上获取的事件类)
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);

        ringBuffer.publish(next);              ///4.发布事件（把这个事件标记为可以消费的事件）
    }
    /**
     * 优雅停机
     * 这段代码的作用是 在应用关闭或 Spring 容器销毁时优雅地关闭 Disruptor
     */
    @PreDestroy
    public void destroy() {
        pictureEditEventDisruptor.shutdown();
    }
}
