package org.example.service.support.special;

import lombok.extern.slf4j.Slf4j;
import org.example.model.session.UserSession;
import org.example.sender.service.SendMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import static org.example.util.MessageProperties.UNKNOWN_MESSAGE;

@Slf4j
@Component
public class UnknownMessageService {

    @Autowired
    private SendMessageService sendMessageService;

    // This method use when message with another type than text or photo was received
    public void unknownMessage(UserSession session, Message message) {
        log.info("Get not typical message from {}", message.getChatId());
        sendMessageService.sendMessageNotSaveMessage(
                session.getChatId(), UNKNOWN_MESSAGE, session.getLocale()
        );
    }
}
