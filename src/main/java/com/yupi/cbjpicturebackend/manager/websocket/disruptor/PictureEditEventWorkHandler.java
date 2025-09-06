package com.yupi.cbjpicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.yupi.cbjpicturebackend.manager.websocket.PictureEditHandler;
import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

import static com.yupi.cbjpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum.*;


/**
 * 图片编辑事件处理（消费者）
 * 把PictureEditHandler分发消息逻辑搬了过来
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private PictureEditHandler pictureEditHandler;
    /// 每当生产者发布事件后，消费者处理事件的入口
    ///  由 Disruptor 内部线程自动调用
    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        log.info("disruptor开始进行消费");
        PictureEditRequestMessage pictureEditResponseMessage = pictureEditEvent.getPictureEditRequestMessage();

        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        String type = pictureEditResponseMessage.getType();
        PictureEditMessageTypeEnum enumByValue = getEnumByValue(type);

        /// 根据消息类型处理消息
        switch (enumByValue) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditResponseMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditResponseMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditResponseMessage, session, user, pictureId);
                break;
            default:
                log.error("不存在该消息类型 {} ",enumByValue);
                break;
        }
        log.info("disruptor消费结束");
    }
}
