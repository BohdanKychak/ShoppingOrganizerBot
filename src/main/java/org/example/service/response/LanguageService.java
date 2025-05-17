package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.enums.Language;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.session.UserSession;
import org.example.repository.UserDataRepository;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.example.action.tag.InlineButtonKey.BACK_REMOVE_BUTTON;
import static org.example.action.tag.InlineButtonKey.SETTINGS_LANGUAGE_BUTTON;
import static org.example.enums.Language.getLanguageCodes;
import static org.example.service.support.PropertiesService.getLocale;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.ACTIVE;
import static org.example.util.Constants.INACTIVE;
import static org.example.util.Constants.LANGUAGE_STRING_LIST_FORMAT;
import static org.example.util.MessageProperties.SETTINGS_LANGUAGE_CHANGE_TAB_MESSAGE;
import static org.example.util.MessageProperties.SETTINGS_LANGUAGE_OPEN_TAB_MESSAGE;

@Slf4j
@Component
public class LanguageService {

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    public void openLanguageTab(UserSession session, Integer messageId) {
        log.info("Opening language tab for chatId: {}", session.getChatId());
        session.getPath().push(SETTINGS_LANGUAGE_BUTTON);
        updateMessageService.editInlineMessage(
                session.getChatId(), messageId,
                session.getLocale(), SETTINGS_LANGUAGE_OPEN_TAB_MESSAGE
        );
        sendMessageService.sendMessage(session.getChatId(), getChangeLanguageString(session.getLocale()));
        workWithLanguageTab(session, session.getLastMessageId());
    }

    private String getChangeLanguageString(Locale locale) {
        return getMessage(SETTINGS_LANGUAGE_CHANGE_TAB_MESSAGE, locale) + getLanguagesStringList();
    }

    public void changeLanguage(UserSession session, Update update) {
        log.info("Changing language for chatId: {}", session.getChatId());
        String stringLocale = update.getCallbackQuery().getData();
        Locale locale = getLocale(stringLocale);

        if (session.getLocale().equals(locale)) return;

        session.setLocale(locale);
        userDataRepository.updateLocaleByChatId(session.getChatId(), stringLocale);

        log.info("Updating language tab for chatId: {}", session.getChatId());
        workWithLanguageTab(session, update.getCallbackQuery().getMessage().getMessageId());
    }

    private String getLanguagesStringList() {
        StringBuilder languageStringList = new StringBuilder();
        for (Language language : Language.values()) {
            languageStringList.append(String.format(LANGUAGE_STRING_LIST_FORMAT,
                    language.getCode().toUpperCase(), language.getName()));
        }
        return languageStringList.toString();
    }

    private void workWithLanguageTab(UserSession session, Integer messageId) {
        List<String> langList = getLanguageCodes();
        List<String> langButtonList = getLanguageButtons(session.getLocale(), langList);
        log.info("Send language menu for chatId: {}", session.getChatId());
        updateMessageService.editInlineMessage(session.getChatId(), messageId,
                null, getChangeLanguageString(session.getLocale()),
                inlineCommandHelper.buildInlineKeyboardMaxThreeButton(session.getLocale(),
                        langButtonList, langList, BACK_REMOVE_BUTTON, null)
        );
    }

    private List<String> getLanguageButtons(Locale locale, List<String> langList) {
        return langList.stream()
                .map(lang -> lang + (lang.equals(locale.toString()) ? ACTIVE : INACTIVE))
                .collect(Collectors.toList());
    }

}
