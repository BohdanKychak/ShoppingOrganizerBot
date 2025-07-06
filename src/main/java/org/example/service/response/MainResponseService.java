package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.helper.keyboard.ReplyKeyboardHelper;
import org.example.model.session.UserSession;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static org.example.action.tag.InlineButtonKey.*;
import static org.example.action.tag.ReplyButtonKey.MAIN_INCOME_REPLY;
import static org.example.action.tag.ReplyButtonKey.MAIN_PROFILE_REPLY;
import static org.example.action.tag.ReplyButtonKey.MAIN_PURCHASE_REPLY;
import static org.example.action.tag.ReplyButtonKey.MAIN_QUICK_PURCHASE_REPLY;
import static org.example.bot.Dispatcher.USER_SESSION_MAP;
import static org.example.util.MessageProperties.MAIN_MENU_MESSAGE;
import static org.example.util.MessageProperties.MAIN_PROFILE_MESSAGE;
import static org.example.util.MessageProperties.RESTART_BOT_MESSAGE;
import static org.example.util.MessageProperties.TEMPORARY_MESSAGE_WAIT_MESSAGE;
import static org.example.util.MessageProperties.TRANSACTION_SEARCH_STOPPED_MESSAGE;

@Slf4j
@Component
public class MainResponseService {

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private ReplyKeyboardHelper replyKeyboardHelper;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Autowired
    private MoneyTransactionService moneyTransactionService;

    public void sendMessageWithMainCommands(Long chatId, String key, List<String> params, Locale locale) {
        sendMessageService.sendMessage(chatId, key, params, locale,
                replyKeyboardHelper.buildKeyboardTwoPerLine(locale,
                        List.of(
                                MAIN_QUICK_PURCHASE_REPLY,
                                MAIN_INCOME_REPLY,
                                MAIN_PURCHASE_REPLY,
                                MAIN_PROFILE_REPLY))
        );
    }

    public void restartUser(UserSession session) {
        log.info("Deleting user session for chatId: {}", session.getChatId());
        if (USER_SESSION_MAP.get(session.getChatId()) != null) USER_SESSION_MAP.remove(session.getChatId());
        sendMessageService.sendMessage(session.getChatId(), RESTART_BOT_MESSAGE, session.getLocale());
    }

    public void toMain(UserSession userSession) {
        if (wasPurchaseFilter(userSession)) {
            log.info("Stop purchase filter for chatId: {}", userSession.getChatId());
            updateMessageService.editInlineMessage(
                    userSession.getChatId(), userSession.getLastMessageId(),
                    userSession.getLocale(), TRANSACTION_SEARCH_STOPPED_MESSAGE
            );
        }
        log.info("Clear all temporary data and redirect to main for user {}", userSession.getChatId());
        userSession.clearAllTemporaryData();
        sendMessageWithMainCommands(userSession.getChatId(), MAIN_MENU_MESSAGE, List.of(), userSession.getLocale());
    }

    public void quickPurchaseTab(UserSession userSession) {
        moneyTransactionService.quickPurchaseTab(userSession);
    }

    public void purchaseTab(UserSession userSession) {
        moneyTransactionService.purchaseTab(userSession);
    }

    public void incomeTab(UserSession userSession) {
        moneyTransactionService.incomeTab(userSession);
    }

    public void profile(UserSession userSession) {
        log.info("Open profile tab for chatId: {}", userSession.getChatId());
        userSession.getPath().clear();
        userSession.getPath().push(MAIN_PROFILE_REPLY);
        sendMessageService.sendTemporaryMessage(
                userSession.getChatId(),
                TEMPORARY_MESSAGE_WAIT_MESSAGE,
                userSession.getLocale()
        );
        sendMessageService.sendMessage(
                userSession.getChatId(),
                MAIN_PROFILE_MESSAGE,
                userSession.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(
                        userSession.getLocale(),
                        List.of(FAMILY_SETTINGS_BUTTON, FAMILY_TRANSACTIONS_BUTTON,
                                ACCOUNT_BUTTON, GENERAL_SETTINGS_BUTTON), BACK_REMOVE_BUTTON
                )
        );
    }

    private boolean wasPurchaseFilter(UserSession session) {
        return session.getPath().contains(PURCHASES_FILTER_BUTTON);
    }
}
