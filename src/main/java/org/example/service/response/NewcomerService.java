package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.helper.keyboard.ReplyKeyboardHelper;
import org.example.model.entity.UserData;
import org.example.model.session.UserSession;
import org.example.repository.UserDataRepository;
import org.example.sender.service.SendMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.example.action.tag.CommandKey.FAMILY_COMMAND;
import static org.example.action.tag.CommandKey.START_COMMAND;
import static org.example.action.tag.ReplyButtonKey.FAMILY_CREATE_REPLY;
import static org.example.action.tag.ReplyButtonKey.FAMILY_JOIN_REPLY;
import static org.example.bot.Dispatcher.USER_SESSION_MAP;
import static org.example.service.support.PropertiesService.getLocale;
import static org.example.util.Constants.SEPARATOR;
import static org.example.util.MessageProperties.PLEASE_ENTER_PASSWORD_MESSAGE;
import static org.example.util.MessageProperties.WELCOME_AND_SET_FAMILY__FORMAT_MESSAGE;
import static org.example.util.MessageProperties.WELCOME_BACK__FORMAT_MESSAGE;

@Slf4j
@Component
public class NewcomerService {

    private static final int PASSWORD_MAX_LENGTH = 20;
    private static final int NUMBER_OF_PASSWORD_PARTS = 2;

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private MainResponseService mainResponseService;

    @Autowired
    private ReplyKeyboardHelper replyKeyboardHelper;

    @SuppressWarnings("unused")
    @Value("${telegram.bot.password}")
    private String botPassword;

    public void checkNewUser(Message message) {
        Long chatId = message.getChatId();
        log.info("Attempting to start work with user {}", chatId);
        Locale locale = getLocale(message.getFrom().getLanguageCode());

        if (isPasswordCorrect(message.getText())) {
            log.debug("User {} passed the password check.", chatId);
            String firstName = message.getFrom().getFirstName();
            USER_SESSION_MAP.put(chatId,
                    UserSession.builder()
                            .chatId(chatId)
                            .name(firstName)
                            .build()
            );

            Optional<UserData> oUser = userDataRepository.findById(chatId);
            if (oUser.isPresent() && oUser.get().getFamilyId() != null) {
                log.info("{}-{} log-in.", firstName, chatId);
                oldUser(chatId, firstName, locale, oUser.get());
            } else {
                log.info("{}-{} sign-up.", firstName, chatId);
                newUser(chatId, firstName, locale, oUser.isPresent());
            }
        } else {
            log.info("User {} is not registered.", chatId);
            sendMessageService.sendMessage(chatId, PLEASE_ENTER_PASSWORD_MESSAGE, locale);
        }
    }

    private void oldUser(Long chatId, String firstName, Locale locale, UserData oUser) {
        locale = checkUserDataLocale(chatId, oUser, locale);

        UserSession user = USER_SESSION_MAP.get(chatId);
        user.setLocale(locale);
        user.setFamilyId(oUser.getFamilyId());
        mainResponseService.sendMessageWithMainCommands(
                chatId, WELCOME_BACK__FORMAT_MESSAGE, List.of(firstName), locale);
    }

    private void newUser(Long chatId, String firstName, Locale locale, boolean isRegistered) {
        USER_SESSION_MAP.get(chatId).getPath().add(FAMILY_COMMAND);
        USER_SESSION_MAP.get(chatId).setLocale(locale);
        USER_SESSION_MAP.get(chatId).userIsBusy();

        if (!isRegistered) {
            userDataRepository.save(new UserData(chatId, firstName, locale.toString()));
        }
        sendMessageService.sendMessage(chatId,
                WELCOME_AND_SET_FAMILY__FORMAT_MESSAGE,
                List.of(firstName),
                locale,
                replyKeyboardHelper.buildKeyboardOnePerLine(locale,
                        List.of(FAMILY_CREATE_REPLY, FAMILY_JOIN_REPLY))
        );
    }

    private boolean isPasswordCorrect(String password) {
        if (password != null && password.length() < PASSWORD_MAX_LENGTH) {
            String[] passwordParts = password.split(SEPARATOR);
            return passwordParts.length == NUMBER_OF_PASSWORD_PARTS
                    && START_COMMAND.equals(passwordParts[0].trim())
                    && botPassword.equals(passwordParts[1]);
        }
        return false;
    }

    private Locale checkUserDataLocale(Long chatId, UserData oUser, Locale locale) {
        String oLocale = oUser.getLocale();
        if (oLocale == null) {
            userDataRepository.updateLocaleByChatId(chatId, locale.toString());
            log.info("Locale for chatId {} is updated to {}.", chatId, locale);
        } else {
            locale = getLocale(oLocale);
        }
        return locale;
    }
}
