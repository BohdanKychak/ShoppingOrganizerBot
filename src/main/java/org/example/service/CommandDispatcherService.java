package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.action.handler.CommandResponseHandler;
import org.example.action.handler.ReplyButtonHandler;
import org.example.model.session.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.example.service.support.PropertiesService.getButtonCodeByText;
import static org.example.util.Constants.COMMAND_SYMBOL;

@Slf4j
@Component
public class CommandDispatcherService {

    @Autowired
    private CommandResponseHandler commandResponseHandler;

    @Autowired
    private ReplyButtonHandler replyButtonHandler;

    public void workWithCommands(UserSession userSession, Update update) {
        String receivedText = update.getMessage().getText();
        if (receivedText.startsWith(COMMAND_SYMBOL)) {
            log.debug("Message {} from {} redirect to command handler", update.getMessage().getMessageId(), userSession.getChatId());
            commandResponseHandler.handleCommand(userSession, update);
        } else {
            String replyButton = getReplyButtonCode(userSession, receivedText);
            if (replyButton != null) {
                update.getMessage().setText(replyButton);
            }
            log.debug("Message {} from {} redirect to reply button handler", update.getMessage().getMessageId(), userSession.getChatId());
            replyButtonHandler.handleReplyButton(userSession, update);
        }
    }

    private String getReplyButtonCode(UserSession session, String receivedText) {
        return getButtonCodeByText(receivedText, session.getLocale());
    }

}
