package org.example.action.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.action.Action;
import org.example.model.session.UserSession;
import org.example.service.response.AccountService;
import org.example.service.response.CurrencyService;
import org.example.service.response.FamilySettingsService;
import org.example.service.response.LanguageService;
import org.example.service.response.MoneyTransactionService;
import org.example.service.response.UserSettingsTabService;
import org.example.service.response.transaction_info.purchase.PurchaseFilterService;
import org.example.service.response.transaction_info.purchase.PurchaseLastService;
import org.example.service.response.transaction_info.purchase.PurchaseSortedService;
import org.example.service.support.special.SpecialInlineResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.example.action.tag.InlineButtonKey.*;
import static org.example.enums.Currency.getCurrencyCodes;
import static org.example.enums.Language.getLanguageCodes;
import static org.example.util.Constants.PURCHASE_GENERATE_PATH_CODE;
import static org.example.util.Constants.PURCHASE_LAST_SPECIAL_CODE;
import static org.example.util.Constants.PURCHASE_SORTED_SPECIAL_CODE;

@Slf4j
@Component
public class InlineButtonHandler {

    @Autowired
    private SpecialInlineResponseService specialInlineResponseService;

    @Autowired
    private FamilySettingsService familySettingsService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private MoneyTransactionService moneyTransactionService;

    @Autowired
    private PurchaseLastService purchaseLastService;

    @Autowired
    private PurchaseFilterService purchaseFilterService;

    @Autowired
    private PurchaseSortedService purchaseSortedService;

    @Autowired
    private UserSettingsTabService userSettingsTabService;

    @Autowired
    private LanguageService languageService;

    private final Map<String, Action> inlineButtonActions = new HashMap<>();

    public InlineButtonHandler() {
        inlineButtonActions.put(BOT_INFO_BUTTON, this::handleBotInfoButton);
        inlineButtonActions.put(BACK_REMOVE_BUTTON, this::handleBackRemoveButton);
        inlineButtonActions.put(BACK_MODIFY_BUTTON, this::handleBackModifyButton);
        inlineButtonActions.put(RULE_BUTTON, this::handleRuleButton);
        inlineButtonActions.put(READY_BUTTON, this::handleReadyButton);
        inlineButtonActions.put(UPDATE_BUTTON, this::handleUpdateButton);

        inlineButtonActions.put(FAMILY_CHANGE_NAME_BUTTON, this::handleFamilyChangeName);
        inlineButtonActions.put(FAMILY_CHANGE_CURRENCY_BUTTON, this::handleFamilyChangeCurrency);
        inlineButtonActions.put(FAMILY_MEMBERS_INFO_BUTTON, this::handleFamilyMembersInfo);
        inlineButtonActions.put(FAMILY_LEAVE_BUTTON, this::handleFamilyLeave);

        inlineButtonActions.put(FAMILY_SETTINGS_BUTTON, this::handleFamilySettings);
        inlineButtonActions.put(FAMILY_TRANSACTIONS_BUTTON, this::handleFamilyTransactions);
        inlineButtonActions.put(ACCOUNT_BUTTON, this::handleAccount);
        inlineButtonActions.put(GENERAL_SETTINGS_BUTTON, this::handleGeneralSettings);

        inlineButtonActions.put(PURCHASES_INFO_BUTTON, this::handlePurchasesInfo);
        inlineButtonActions.put(PURCHASES_FILTER_BUTTON, this::handlePurchasesFilter);
        inlineButtonActions.put(INCOME_INFO_BUTTON, this::handleIncomeInfo);
        inlineButtonActions.put(INCOME_FILTER_BUTTON, this::handleIncomeFilter);

        inlineButtonActions.put(SETTINGS_LANGUAGE_BUTTON, this::handleOpenLanguageTab);

        inlineButtonActions.put(SKIP_PURCHASE_DETAILS_BUTTON, this::handleSkipPurchaseDetails);
        inlineButtonActions.put(NEXT_BUTTON, this::handleNextPurchase);
        inlineButtonActions.put(PREVIOUS_BUTTON, this::handlePreviousPurchase);
        inlineButtonActions.put(STOP_BUTTON, this::handleStopPurchase);
        inlineButtonActions.put(PURCHASE_SELECTED_BUTTON, this::handleSelectedPurchase);
        inlineButtonActions.put(PURCHASE_RECEIPT_BUTTON, this::handlePurchaseReceipt);
        inlineButtonActions.put(PURCHASE_RECEIPT_ADD_BUTTON, this::handlePurchaseAddReceipt);
        inlineButtonActions.put(PURCHASE_RECEIPT_REPLACE_BUTTON, this::handlePurchaseReplaceReceipt);
        inlineButtonActions.put(PURCHASE_DESCRIPTION_BUTTON, this::handlePurchaseDescription);
        inlineButtonActions.put(PURCHASE_DELETE_BUTTON, this::handlePurchaseDelete);
        inlineButtonActions.put(PURCHASE_LAST_SPECIAL_CODE, this::handleRedirectionByPurchaseCode);
        inlineButtonActions.put(PURCHASE_SORTED_SPECIAL_CODE, this::handleRedirectionByPurchaseCode);
        inlineButtonActions.put(PURCHASE_GENERATE_PATH_CODE, this::handleRedirectionByPurchaseCode);

        inlineButtonActions.put(USER_FILTER_BUTTON, this::handleUserFilter);
        inlineButtonActions.put(AMOUNT_FILTER_BUTTON, this::handleAmountFilter);
        inlineButtonActions.put(DATE_FILTER_BUTTON, this::handleDateFilter);
        inlineButtonActions.put(CURRENCY_FILTER_BUTTON, this::handleCurrencyFilter);

        inlineButtonActions.put(USER_FILTER_WITH_BUTTON, this::handleUserFilterChoice);
        inlineButtonActions.put(USER_FILTER_WITHOUT_BUTTON, this::handleUserFilterChoice);
        inlineButtonActions.put(USER_FILTER_ALL_BUTTON, this::handleUserFilterChoice);

        inlineButtonActions.put(CURRENCY_EXCHANGE_RATE_TABLE_BUTTON, this::handleCurrencyExchangeRateTable);
        inlineButtonActions.put(CURRENCY_EXCHANGE_RATE_TEST_BUTTON, this::handleCurrencyExchangeRateTest);

        for (String currency : getCurrencyCodes()) {
            inlineButtonActions.put(currency, this::handleWorkWithCurrency);
        }
        for (String language : getLanguageCodes()) {
            inlineButtonActions.put(language, this::handleWorkWithLanguage);
        }
    }

    public void handleButtonClick(UserSession session, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (session == null || !session.getLastMessageId().equals(messageId)) {
            log.info("Remove old button from user {}", update.getCallbackQuery().getFrom().getId());
            specialInlineResponseService
                    .deleteMessage(update.getCallbackQuery().getFrom().getId(), messageId);
            return;
        }
        String buttonCode = callbackQuery.getData();
        log.info("Received inlineButton \"{}\" with callbackId {} from user {}",
                buttonCode, callbackQuery.getId(), session.getChatId());

        Optional.ofNullable(inlineButtonActions.get(buttonCode))
                .ifPresentOrElse(
                        action -> action.execute(session, update),
                        () -> handleIncorrectButton(session, update)
                );
    }


    private void handleBotInfoButton(UserSession session, Update update) {
        specialInlineResponseService.workWithBotInfoButton(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }

    private void handleBackRemoveButton(UserSession session, Update update) {
        String newData = specialInlineResponseService
                .workWithBackRemoveButton(session, update);
        if (newData != null) {
            update.getCallbackQuery().setData(newData);
            handleButtonClick(session, update);
        }
    }

    private void handleBackModifyButton(UserSession session, Update update) {
        String newData = specialInlineResponseService
                .workWithBackModifyButton(session, update);
        if (newData != null) {
            update.getCallbackQuery().setData(newData);
            handleButtonClick(session, update);
        }
    }

    private void handleRuleButton(UserSession session, Update update) {
        specialInlineResponseService.workWithRuleButton(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }

    private void handleReadyButton(UserSession session, Update update) {
        specialInlineResponseService.workWithReadyButton(session);
    }

    private void handleUpdateButton(UserSession session, Update update) {
        specialInlineResponseService.workWithUpdateButton(session, update);
    }


    private void handleFamilyChangeName(UserSession session, Update update) {
        familySettingsService.openChangeFamilyName(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }

    private void handleFamilyChangeCurrency(UserSession session, Update update) {
        currencyService.changeFamilyCurrency(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }

    private void handleFamilyMembersInfo(UserSession session, Update update) {
        familySettingsService.familyMembersList(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }

    private void handleFamilyLeave(UserSession session, Update update) {
        familySettingsService.leaveFamily(session,
                update.getCallbackQuery().getMessage().getMessageId(), true);
    }


    private void handleFamilySettings(UserSession session, Update update) {
        familySettingsService.workWithFamilySettingButton(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }

    private void handleFamilyTransactions(UserSession session, Update update) {
        moneyTransactionService.transactionsTabInfo(session);
    }

    private void handleAccount(UserSession session, Update update) {
        accountService.getFamilyAccountInfo(session, update);
    }

    private void handleGeneralSettings(UserSession session, Update update) {
        userSettingsTabService.openSettingsTab(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }


    private void handlePurchasesInfo(UserSession session, Update update) {
        purchaseLastService.showPurchaseTableInfo(session, update);
    }

    private void handlePurchasesFilter(UserSession session, Update update) {
        purchaseFilterService.showFilterMenu(session);
    }

    private void handleIncomeInfo(UserSession session, Update update) {
        specialInlineResponseService.workWithNotImplementedButton(session,
                update.getCallbackQuery().getId());
    }

    private void handleIncomeFilter(UserSession session, Update update) {
        specialInlineResponseService.workWithNotImplementedButton(session,
                update.getCallbackQuery().getId());
    }


    private void handleOpenLanguageTab(UserSession session, Update update) {
        languageService.openLanguageTab(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }


    private void handleSkipPurchaseDetails(UserSession session, Update update) {
        boolean workedWithSkip = moneyTransactionService.skipPurchaseDetails(session, update);
        if (!workedWithSkip) {
            handleIncorrectButton(session, update);
        }
    }

    private void handleNextPurchase(UserSession session, Update update) {
        if (!purchaseLastService.scrollNextPurchase(session)) {
            specialInlineResponseService.answerToIncorrectCommand(session);
        }
    }

    private void handlePreviousPurchase(UserSession session, Update update) {
        if (!purchaseLastService.scrollPreviousPurchase(session)) {
            specialInlineResponseService.answerToIncorrectCommand(session);
        }
    }

    private void handleStopPurchase(UserSession session, Update update) {
        purchaseLastService
                .workWithStopButton(session, update.getCallbackQuery().getId());
    }

    private void handleSelectedPurchase(UserSession session, Update update) {
        specialInlineResponseService.workWithSelected(session);
    }

    private void handlePurchaseReceipt(UserSession session, Update update) {
        specialInlineResponseService.workWithShowReceipt(session, update);
    }

    private void handlePurchaseAddReceipt(UserSession session, Update update) {
        specialInlineResponseService.workWithPurchaseAddReceipt(session, update);
    }

    private void handlePurchaseReplaceReceipt(UserSession session, Update update) {
        specialInlineResponseService.workWithPurchaseReplaceReceipt(session, update);
    }

    private void handlePurchaseDescription(UserSession session, Update update) {
        specialInlineResponseService.workWithPurchaseDescription(session, update);
    }

    private void handlePurchaseDelete(UserSession session, Update update) {
        specialInlineResponseService.workWithPurchaseDelete(session, update);
    }

    private void handleRedirectionByPurchaseCode(UserSession session, Update update) {
        if (purchaseLastService.openSelectedPurchaseWithoutMessage(session)) return;
        if (purchaseSortedService.openSelectedPurchaseWithoutMessage(session)) return;
        if (PURCHASE_GENERATE_PATH_CODE.equals(session.getPath().peek())) {
            purchaseSortedService.showPurchaseTableInfo(session, update);
            return;
        }
        specialInlineResponseService.answerToIncorrectCommand(session);
    }


    private void handleUserFilter(UserSession session, Update update) {
        purchaseFilterService.userFilterSetupMenu(session);
    }

    private void handleAmountFilter(UserSession session, Update update) {
        purchaseFilterService.amountFilterSetupMenu(session);
    }

    private void handleDateFilter(UserSession session, Update update) {
        purchaseFilterService.dateFilterSetupMenu(session);
    }

    private void handleCurrencyFilter(UserSession session, Update update) {
        purchaseFilterService.currencyFilterSetupMenu(session);
    }

    private void handleUserFilterChoice(UserSession session, Update update) {
        purchaseFilterService.setUserCriteria(session, update);
    }


    private void handleCurrencyExchangeRateTable(UserSession session, Update update) {
        currencyService.workWithExchangeRate(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }

    private void handleCurrencyExchangeRateTest(UserSession session, Update update) {
        currencyService.turnOnExchangeRateTest(session,
                update.getCallbackQuery().getMessage().getMessageId());
    }


    private void handleWorkWithCurrency(UserSession session, Update update) {
        specialInlineResponseService.workWithCurrencyButtons(session, update);
    }

    private void handleWorkWithLanguage(UserSession session, Update update) {
        languageService.changeLanguage(session, update);
    }


    private void handleIncorrectButton(UserSession session, Update update) {
        log.error("No action found for button code: {}", update.getCallbackQuery().getData());
        specialInlineResponseService.answerToIncorrectCommand(session);
    }

}
