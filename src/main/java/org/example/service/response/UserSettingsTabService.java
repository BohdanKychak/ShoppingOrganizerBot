package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.session.UserSession;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.example.action.tag.InlineButtonKey.BACK_REMOVE_BUTTON;
import static org.example.action.tag.InlineButtonKey.GENERAL_SETTINGS_BUTTON;
import static org.example.action.tag.InlineButtonKey.SETTINGS_LANGUAGE_BUTTON;
import static org.example.util.MessageProperties.SETTINGS_GENERAL_TAB_MESSAGE;

@Slf4j
@Component
public class UserSettingsTabService {

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    public void openSettingsTab(UserSession session, Integer messageId) {
        log.info("Open settings tab for user: {}", session.getChatId());
        session.getPath().push(GENERAL_SETTINGS_BUTTON);
        updateMessageService.updateKeyboard(session.getChatId(), messageId, null);
        sendMessageService.sendMessage(session.getChatId(),
                SETTINGS_GENERAL_TAB_MESSAGE, session.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                        List.of(SETTINGS_LANGUAGE_BUTTON),
                        BACK_REMOVE_BUTTON
                )
        );
    }
}
