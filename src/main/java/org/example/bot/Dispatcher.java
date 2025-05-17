package org.example.bot;

import lombok.extern.slf4j.Slf4j;
import org.example.action.handler.InlineButtonHandler;
import org.example.model.session.UserSession;
import org.example.service.CommandDispatcherService;
import org.example.service.response.NewcomerService;
import org.example.service.response.PictureService;
import org.example.service.support.special.UnknownMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class Dispatcher {

    @Autowired
    private NewcomerService newcomerService;

    @Autowired
    private PictureService pictureService;

    @Autowired
    private CommandDispatcherService commandDispatcherService;

    @Autowired
    private UnknownMessageService unknownMessageService;

    @Autowired
    private InlineButtonHandler inlineButtonHandler;

    public static final Map<Long, UserSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    public void messageResponse(Update update) {
        Message message = update.getMessage();
        UserSession userSession = USER_SESSION_MAP.get(message.getChatId());

        if (userSession == null) newcomerService.checkNewUser(message);
        else if (message.hasPhoto()) {
            log.debug("Get photo: \"{}\" from {}",
                    message.getPhoto().get(0).getFileId(), message.getChatId());
            pictureService.workWithPictures(userSession, message);
        } else if (message.hasText()) {
            log.debug("Get message: \"{}\" from {}",
                    message.getText(), message.getChatId());
            commandDispatcherService.workWithCommands(userSession, update);
        } else {
            unknownMessageService.unknownMessage(userSession, message);
        }
    }

    public void callbackQueryResponse(Update update) {
        UserSession userSession = USER_SESSION_MAP.get(update.getCallbackQuery().getFrom().getId());
        inlineButtonHandler.handleButtonClick(userSession, update);
    }
}
