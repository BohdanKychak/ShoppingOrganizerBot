package org.example.service.support.special;

import lombok.extern.slf4j.Slf4j;
import org.example.enums.Currency;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.entity.Purchase;
import org.example.model.session.UserSession;
import org.example.repository.FamilyDataRepository;
import org.example.repository.PurchaseRepository;
import org.example.sender.service.AnswerMessageService;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.example.service.CommandDispatcherService;
import org.example.service.response.CurrencyService;
import org.example.service.response.MainResponseService;
import org.example.service.response.transaction_info.purchase.PurchaseFilterService;
import org.example.service.response.transaction_info.purchase.PurchaseLastService;
import org.example.service.response.transaction_info.purchase.PurchaseSortedService;
import org.example.service.support.TransactionCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.example.action.tag.CommandKey.START_COMMAND;
import static org.example.action.tag.InlineButtonKey.*;
import static org.example.action.tag.ReplyButtonKey.MAIN_INCOME_REPLY;
import static org.example.action.tag.ReplyButtonKey.MAIN_PURCHASE_REPLY;
import static org.example.action.tag.ReplyButtonKey.MAIN_QUICK_PURCHASE_REPLY;
import static org.example.enums.Currency.getCurrenciesDescriptionList;
import static org.example.service.support.PropertiesService.getInlineButton;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.*;
import static org.example.util.MessageProperties.*;

@Slf4j
@Component
public class SpecialInlineResponseService {

    @Autowired
    private FamilyDataRepository familyDataRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private AnswerMessageService answerMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Autowired
    private MainResponseService mainResponseService;

    @Autowired
    private CommandDispatcherService commandDispatcherService;

    @Autowired
    private PurchaseLastService purchaseLastService;

    @Autowired
    private PurchaseFilterService purchaseFilterService;

    @Autowired
    private PurchaseSortedService purchaseSortedService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private TransactionCacheService transactionCacheService;

    @SuppressWarnings("unused")
    @Value("${telegram.bot.password}")
    private String botPassword;

    @SuppressWarnings("unused")
    @Value("${telegram.bot.username}")
    private String botName;

    private static String CURRENCY_FOR_PATTERN;

    public String workWithBackRemoveButton(UserSession userSession, Update update) {
        log.debug("Move to previous page by BackRemoveButton for user: {}", userSession.getChatId());
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        deleteMessage(userSession.getChatId(), messageId);
        if (!userSession.getPath().isEmpty()) {
            userSession.getPath().pop();
        }
        if (userSession.getPath().isEmpty()) {
            mainResponseService.sendMessageWithMainCommands(
                    userSession.getChatId(), MAIN_MENU_MESSAGE,
                    List.of(), userSession.getLocale()
            );
            return null;
        }
        if (userSession.getPath().size() == 1) {
            Message message = new Message();
            message.setText(userSession.getPath().pop());
            update.setMessage(message);
            commandDispatcherService.workWithCommands(userSession, update);
            return null;
        }
        return userSession.getPath().peek();
    }

    public String workWithBackModifyButton(UserSession userSession, Update update) {
        log.debug("Move to previous page by BackModifyButton for user: {}", userSession.getChatId());
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        updateMessageService.updateKeyboard(userSession.getChatId(), messageId, null);
        if (!userSession.getPath().isEmpty()) {
            userSession.getPath().pop();
        }
        if (userSession.getPath().isEmpty()) {
            mainResponseService.sendMessageWithMainCommands(
                    userSession.getChatId(), MAIN_MENU_MESSAGE,
                    List.of(), userSession.getLocale()
            );
            return null;
        }
        if (userSession.getPath().size() == 1) {
            deleteMessage(userSession.getChatId(), messageId);
            Message message = new Message();
            message.setText(userSession.getPath().pop());
            update.setMessage(message);
            commandDispatcherService.workWithCommands(userSession, update);
            return null;
        }
        return userSession.getPath().peek();
    }

    public void workWithRuleButton(UserSession session, Integer messageId) {
        updateMessageService.updateKeyboard(session.getChatId(), messageId, null);
        String lastCommand = session.getPath().peek();
        log.info("Open {} rule button for user: {}", lastCommand, session.getChatId());
        if (CURRENCY_EXCHANGE_RATE_TEST_BUTTON.equals(lastCommand)) {
            sendMessageService.sendMessage(session.getChatId(),
                    RULE_EXCHANGE_RATE_TEST__FORMAT_MESSAGE, List.of(getCurrenciesDescriptionList()),
                    session.getLocale(),
                    inlineCommandHelper.buildInlineKeyboard(
                            session.getLocale(), List.of(FAMILY_SETTINGS_BUTTON)));
            return;
        }
        if (MAIN_QUICK_PURCHASE_REPLY.equals(lastCommand)) {
            sendMessageService.sendMessage(session.getChatId(),
                    RULE_QUICK_PURCHASE__FORMAT_MESSAGE, getCurrencyForPattern(),
                    session.getLocale(),
                    inlineCommandHelper.buildInlineKeyboard(
                            session.getLocale(), List.of(BACK_REMOVE_BUTTON)));
            return;
        }
        if (MAIN_PURCHASE_REPLY.equals(lastCommand)) {
            sendMessageService.sendMessage(session.getChatId(),
                    RULE_PURCHASE__FORMAT_MESSAGE, getCurrencyForPattern(),
                    session.getLocale(),
                    inlineCommandHelper.buildInlineKeyboard(
                            session.getLocale(), List.of(BACK_REMOVE_BUTTON)));
            return;
        }
        if (MAIN_INCOME_REPLY.equals(lastCommand)) {
            sendMessageService.sendMessage(session.getChatId(),
                    RULE_INCOME__FORMAT_MESSAGE, getCurrencyForPattern(),
                    session.getLocale(),
                    inlineCommandHelper.buildInlineKeyboard(
                            session.getLocale(), List.of(BACK_REMOVE_BUTTON)));
            return;
        }
        if (PURCHASES_INFO_BUTTON.equals(lastCommand)) {
            updateMessageService.editInlineMessage(session.getChatId(),
                    session.getLastMessageId(), null,
                    getTransactionRule(session.getLocale(), RULES_APPENDIX_PURCHASES_INFO_MESSAGE),
                    inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                            List.of(PURCHASES_INFO_BUTTON)));
            return;
        }
        if (PURCHASE_GENERATE_PATH_CODE.equals(lastCommand)
                || RULE_TRANSACTIONS_SORTED__FORMAT_MESSAGE.equals(lastCommand)) {
            if (session.getTransactionSession().getPurchaseSortedHistory() != null) {
                session.getPath().push(RULE_TRANSACTIONS_SORTED__FORMAT_MESSAGE);
                updateMessageService.editInlineMessage(session.getChatId(),
                        session.getLastMessageId(), null,
                        getTransactionsSortedRule(session.getLocale()),
                        inlineCommandHelper.buildInlineKeyboard(
                                session.getLocale(), BACK_MODIFY_BUTTON)
                );
                return;
            }
        }
        if (PURCHASES_FILTER_BUTTON.equals(lastCommand)) {
            session.getPath().push(RULE_BUTTON);
            updateMessageService.editInlineMessage(
                    session.getChatId(), session.getLastMessageId(),
                    session.getLocale(), RULE_TRANSACTIONS_FILTER_MESSAGE,
                    inlineCommandHelper
                            .buildInlineKeyboard(session.getLocale(), BACK_MODIFY_BUTTON));
            return;
        }
        if (AMOUNT_FILTER_BUTTON.equals(lastCommand)) {
            session.getPath().push(RULE_BUTTON);
            updateMessageService.editInlineMessage(
                    session.getChatId(), session.getLastMessageId(),
                    session.getLocale(), RULE_FILTER_AMOUNT_MESSAGE,
                    inlineCommandHelper
                            .buildInlineKeyboard(session.getLocale(), BACK_MODIFY_BUTTON));
            return;
        }
        if (DATE_FILTER_BUTTON.equals(lastCommand)) {
            updateMessageService.deleteMessage(session.getChatId(),
                    Integer.parseInt(session.getTemporaryData()));
            session.setTemporaryData(null);

            session.getPath().push(RULE_BUTTON);
            updateMessageService.editInlineMessage(
                    session.getChatId(), session.getLastMessageId(),
                    session.getLocale(), RULE_FILTER_DATE_MESSAGE,
                    inlineCommandHelper
                            .buildInlineKeyboard(session.getLocale(), BACK_MODIFY_BUTTON));
            return;
        }
        if (CURRENCY_FILTER_BUTTON.equals(lastCommand)) {
            session.getPath().push(RULE_BUTTON);
            updateMessageService.editInlineMessage(
                    session.getChatId(), session.getLastMessageId(),
                    session.getLocale(), RULE_FILTER_CURRENCY_MESSAGE,
                    inlineCommandHelper
                            .buildInlineKeyboard(session.getLocale(), BACK_MODIFY_BUTTON));
            return;
        }
    }

    public void workWithReadyButton(UserSession session) {
        String lastCommand = session.getPath().peek();
        if (CURRENCY_FILTER_BUTTON.equals(lastCommand)) {
            purchaseFilterService.setCurrencyCriteria(session);
        }
    }

    public void workWithUpdateButton(UserSession session, Update update) {
        String lastCommand = session.getPath().peek();
        if (PURCHASES_INFO_BUTTON.equals(lastCommand)) {
            if (checkForUpdates(session)) {
                session.getTransactionSession().tableUpdate();
                purchaseLastService.showPurchaseTableInfo(session, update);
                log.info("Update purchaseLast table by UpdateButton for user: {}", session.getChatId());
            } else {
                answerMessageService.sendSmallWindowAnswer(update.getCallbackQuery().getId(),
                        PURCHASES_TABLE_NO_CHANGED_MESSAGE, session.getLocale());
            }
        }
    }

    public void workWithSelected(UserSession session) {
        session.getPath().pop();
        String command = session.getPath().peek();
        if (PURCHASE_RECEIPT_BUTTON.equals(command)) {
            session.getPath().pop();
            command = session.getPath().peek();
        }

        if (PURCHASE_LAST_SPECIAL_CODE.equals(command)) {
            purchaseLastService.openSelectedPurchase(session,
                    session.getTransactionSession().getPurchaseLastHistory().getPurchase().getId());
        }
        if (PURCHASE_SORTED_SPECIAL_CODE.equals(command)) {
            purchaseSortedService.openSelectedPurchase(session,
                    session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase().getId());
        }
    }

    public void workWithShowReceipt(UserSession session, Update update) {
        String command = session.getPath().peek();

        if (PURCHASE_LAST_SPECIAL_CODE.equals(command)) {
            purchaseLastService.showReceipt(session, update);
        }
        if (PURCHASE_SORTED_SPECIAL_CODE.equals(command)) {
            purchaseSortedService.showReceipt(session, update);
        }
    }

    public void workWithPurchaseAddReceipt(UserSession session, Update update) {
        String command = session.getPath().peek();
        if (PURCHASE_LAST_SPECIAL_CODE.equals(command)) {
            purchaseLastService
                    .receiptUpdateRequest(session, update, PURCHASE_RECEIPT_ADD_BUTTON);
            return;
        }
        if (PURCHASE_SORTED_SPECIAL_CODE.equals(command)) {
            purchaseSortedService
                    .receiptUpdateRequest(session, update, PURCHASE_RECEIPT_ADD_BUTTON);
        }
    }

    public void workWithPurchaseReplaceReceipt(UserSession session, Update update) {
        session.getPath().pop();
        String command = session.getPath().peek();
        if (PURCHASE_LAST_SPECIAL_CODE.equals(command)) {
            purchaseLastService
                    .receiptUpdateRequest(session, update, PURCHASE_RECEIPT_REPLACE_BUTTON);
            return;
        }
        if (PURCHASE_SORTED_SPECIAL_CODE.equals(command)) {
            purchaseSortedService
                    .receiptUpdateRequest(session, update, PURCHASE_RECEIPT_REPLACE_BUTTON);
        }
    }

    public void workWithPurchaseDescription(UserSession session, Update update) {
        String command = session.getPath().peek();
        if (PURCHASE_LAST_SPECIAL_CODE.equals(command)) {
            purchaseLastService.descriptionUpdateRequest(session, update);
            return;
        }
        if (PURCHASE_SORTED_SPECIAL_CODE.equals(command)) {
            purchaseSortedService.descriptionUpdateRequest(session, update);
        }
    }

    public void workWithPurchaseDelete(UserSession session, Update update) {
        String command = session.getPath().peek();
        if (PURCHASE_LAST_SPECIAL_CODE.equals(command)) {
            purchaseLastService.deletePurchaseRequest(session, update);
            return;
        }
        if (PURCHASE_SORTED_SPECIAL_CODE.equals(command)) {
            purchaseSortedService.deletePurchaseRequest(session, update);
        }
    }

    public void workWithCurrencyButtons(UserSession session, Update update) {
        String lastCommand = session.getPath().peek();
        if (FAMILY_CHANGE_CURRENCY_BUTTON.equals(lastCommand)) {
            currencyService.workWithCurrency(session,
                    update.getCallbackQuery().getData(),
                    update.getCallbackQuery().getMessage().getMessageId()
            );
        }
        if (CURRENCY_FILTER_BUTTON.equals(lastCommand)) {
            purchaseFilterService.processCurrencySetup(session, update);
        }
    }

    private static String getTransactionRule(Locale locale, String rulesAppendix) {
        return String.format(
                getMessage(RULE_TRANSACTIONS_LAST__FORMAT_MESSAGE, locale),
                TRANSACTIONS_TABLE_LIMIT, TRANSACTIONS_LIST_LIMIT, UNAVAILABLE, AVAILABLE
        ) + NEW_LINE + getMessage(rulesAppendix, locale);
    }

    private static String getTransactionsSortedRule(Locale locale) {
        return String.format(
                getMessage(RULE_TRANSACTIONS_SORTED__FORMAT_MESSAGE, locale),
                TRANSACTIONS_SORTED_TABLE_LIMIT, INACTIVE, ACTIVE
        );
    }

    private boolean checkForUpdates(UserSession session) {
        Purchase firstOldPurchase = transactionCacheService.getLatestPurchase(session);
        Purchase firstNewPurchase = purchaseRepository
                .findLatestPurchaseByFamilyId(session.getFamilyId());
        return isTableUpdated(firstOldPurchase, firstNewPurchase);
    }

    private boolean isTableUpdated(Purchase firstOldPurchase, Purchase firstNewPurchase) {
        if (firstOldPurchase == null && firstNewPurchase == null) {
            return false;
        }
        return firstOldPurchase == null || firstNewPurchase == null ||
                !firstOldPurchase.getId().equals(firstNewPurchase.getId());
    }


    public void workWithBotInfoButton(UserSession userSession, Integer messageId) {
        updateMessageService.updateKeyboard(
                userSession.getChatId(), messageId, null);
        if (userSession.getPath().isEmpty()
                || !userSession.getPath().peek().equals(FAMILY_MEMBERS_INFO_BUTTON)) {
            return;
        }

        log.debug("Open bot info button for user: {}", userSession.getChatId());
        sendMessageService.sendMessageWithExpirationDate(
                userSession.getChatId(),
                BOT_INFO__FORMAT_MESSAGE,
                List.of(String.format(BOT_TAG_FORMAT, botName),
                        getFullPass(), getFamilyCode(userSession.getFamilyId())),
                userSession.getLocale(),
                getBotInfoButtons(userSession)
        );
    }

    private InlineKeyboardMarkup getBotInfoButtons(UserSession userSession) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        InlineKeyboardButton shareButton = new InlineKeyboardButton();
        shareButton.setText(getInlineButton(BOT_SHARE_BUTTON, userSession.getLocale()));
        shareButton.setSwitchInlineQuery(
                String.format(BOT_LINK__FORMAT, botName, SEPARATOR + botPassword));
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(getInlineButton(BACK_REMOVE_BUTTON, userSession.getLocale()));
        backButton.setCallbackData(BACK_REMOVE_BUTTON);

        keyboard.setKeyboard(Collections.singletonList(List.of(shareButton, backButton)));
        return keyboard;
    }

    public void workWithNotImplementedButton(UserSession userSession, String callbackId) {
        answerMessageService.sendSmallWindowAnswer(callbackId,
                IN_DEVELOPMENT_MESSAGE, userSession.getLocale());
    }

    public void answerToIncorrectCommand(UserSession session) {
        sendMessageService.sendMessageNotSaveMessage(session.getChatId(),
                INCORRECT_COMMAND_MESSAGE, session.getLocale());
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        updateMessageService.deleteMessage(chatId, messageId);
    }

    private String getFullPass() {
        return START_COMMAND + SEPARATOR + botPassword;
    }

    private String getFamilyCode(Long familyId) {
        return familyId + SEPARATOR + familyDataRepository.findFamilyById(familyId).getPassCode();
    }


    private List<String> getCurrencyForPattern() {
        if (CURRENCY_FOR_PATTERN == null) {
            StringBuilder builder = new StringBuilder();
            for (Currency currency : Currency.values()) {
                Set<String> currencyInfo = new LinkedHashSet<>();
                currencyInfo.add(currency.getCode().toLowerCase());
                currencyInfo.add(currency.getSymbol().toLowerCase());
                currencyInfo.add(currency.getName().toLowerCase());
                currencyInfo.add(currency.getShortName().toLowerCase());
                currencyInfo.add(currency.getUsed().toLowerCase());

                for (String s : currencyInfo) {
                    builder.append(s).append(COMMA_SEPARATED_FORMAT);
                }
                builder.setLength(builder.length() - 2);
                builder.append(NEW_LINE);
            }
            CURRENCY_FOR_PATTERN = builder.toString();
        }
        return List.of(CURRENCY_FOR_PATTERN, TRANSACTIONS_SORTED_TABLE_LIMIT.toString());
    }
}
