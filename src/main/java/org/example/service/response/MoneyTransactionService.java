package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.enums.Currency;
import org.example.exception.AccountNotExistException;
import org.example.exception.AmountException;
import org.example.exception.CurrencyNotExistException;
import org.example.exception.NegativeAccountException;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.helper.keyboard.ReplyKeyboardHelper;
import org.example.model.entity.Income;
import org.example.model.entity.Purchase;
import org.example.model.entity.UserData;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseCreation;
import org.example.repository.FamilyDataRepository;
import org.example.repository.IncomeRepository;
import org.example.repository.PurchaseRepository;
import org.example.repository.UserDataRepository;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.example.service.support.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.example.action.tag.InlineButtonKey.*;
import static org.example.action.tag.ReplyButtonKey.*;
import static org.example.util.Constants.*;
import static org.example.util.MessageProperties.*;

@Slf4j
@Component
public class MoneyTransactionService {

    private static final String PURCHASE_LOG_EXCEPTION = "purchase message is incorrect, {}";
    private static final String INCOME_LOG_EXCEPTION = "income message is incorrect, {}";

    private static final List<String> SIMPLE_DESCRIPTION_OPTIONS =
            List.of(DESCRIPTION_PRODUCTS_REPLY, DESCRIPTION_GIFT_REPLY);

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private FamilyDataRepository familyDataRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Autowired
    private ReplyKeyboardHelper replyKeyboardHelper;

    public void quickPurchaseTab(UserSession session) {
        log.info("Open quick purchase tab for user {}", session.getChatId());
        sendWaitingMessage(session);
        session.getPath().clear();
        session.getPath().push(MAIN_QUICK_PURCHASE_REPLY);
        sendMessageService.sendMessage(session.getChatId(),
                MAIN_QUICK_PURCHASE_MESSAGE, session.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(
                        session.getLocale(), List.of(RULE_BUTTON, BACK_REMOVE_BUTTON)));
    }

    public void purchaseTab(UserSession session) {
        log.info("Open purchase tab for user {}", session.getChatId());
        sendWaitingMessage(session);
        session.getPath().push(MAIN_PURCHASE_REPLY);
        sendMessageService.sendMessage(session.getChatId(),
                MAIN_PURCHASE_MESSAGE, session.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(
                        session.getLocale(), List.of(RULE_BUTTON, BACK_REMOVE_BUTTON)));
    }

    public void incomeTab(UserSession session) {
        log.info("Open income tab for user {}", session.getChatId());
        sendWaitingMessage(session);
        session.getPath().push(MAIN_INCOME_REPLY);
        sendMessageService.sendMessage(session.getChatId(),
                MAIN_INCOME_MESSAGE, session.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(
                        session.getLocale(), List.of(RULE_BUTTON, BACK_REMOVE_BUTTON)));
    }

    public boolean moneyTransactionMessage(UserSession session, String receivedText) {
        log.debug("Check message {} in transaction tab", receivedText);
        if (MAIN_QUICK_PURCHASE_REPLY.equals(session.getPath().peek())) {
            log.info("Open quick purchase transaction for user {}", session.getChatId());
            quickPurchaseTransaction(session, receivedText);
            return true;
        }
        if (MAIN_PURCHASE_REPLY.equals(session.getPath().peek())) {
            if (session.getTransactionSession().getPurchaseCreation() == null) {
                log.info("Open purchase transaction for user {}", session.getChatId());
                purchaseTransaction(session, receivedText, false);
                return true;
            }
            if (session.getTransactionSession().getPurchaseCreation().getHasDescription() == null) {
                log.info("Set purchase description for user {}", session.getChatId());
                setPurchaseDescription(session, receivedText);
                return true;
            }
            return false;
        }
        if (MAIN_INCOME_REPLY.equals(session.getPath().peek())) {
            log.info("Open income transaction for user {}", session.getChatId());
            incomeTransaction(session, receivedText);
            return true;
        }
        return false;
    }

    public boolean skipPurchaseDetails(UserSession session, Update update) {
        log.info("Try to skip purchase details for user {}", session.getChatId());
        updateMessageService.updateKeyboard(session.getChatId(),
                update.getCallbackQuery().getMessage().getMessageId(), null);
        if (session.getTransactionSession().getPurchaseCreation() != null) {
            if (session.getTransactionSession().getPurchaseCreation().getHasDescription() == null) {
                log.info("Skip purchase description for user {}", session.getChatId());
                session.getTransactionSession().getPurchaseCreation().skipDescription();
                endOfPurchaseDescription(session, PURCHASE_DETAILS_NO_ADDED_MESSAGE);
                return true;
            }
            if (session.getTransactionSession().getPurchaseCreation().getReceiptPhotoId() == null) {
                log.info("Skip purchase receipt for user {}", session.getChatId());
                endOfPurchaseReceipt(session, PURCHASE_DETAILS_NO_ADDED_MESSAGE);
                return true;
            }
        }
        log.info("Skip purchase details not expected for user {}", session.getChatId());
        return false;
    }

    public void quickPurchaseTransaction(UserSession session, String purchaseString) {
        purchaseTransaction(session, purchaseString, true);
    }

    public void transactionsTabInfo(UserSession session) {
        log.info("Open transactions info tab for user {}", session.getChatId());
        if (PURCHASES_FILTER_BUTTON.equals(session.getPath().peek())) {
            session.getPath().pop();
        }
        session.getPath().push(FAMILY_TRANSACTIONS_BUTTON);
        session.getTransactionSession().clearAll();
        updateMessageService.editInlineMessage(
                session.getChatId(), session.getLastMessageId(),
                session.getLocale(), TRANSACTIONS_INFO_MESSAGE,
                inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                        List.of(PURCHASES_INFO_BUTTON, PURCHASES_FILTER_BUTTON,
                                INCOME_INFO_BUTTON, INCOME_FILTER_BUTTON), BACK_REMOVE_BUTTON
                )
        );
    }

    // call from PictureService
    public void setPurchaseReceiptPhoto(UserSession session, String photoId) {
        updateMessageService.updateKeyboard(
                session.getChatId(), session.getLastMessageId(), null);
        session.getTransactionSession().getPurchaseCreation().addReceiptPhotoId(photoId);

        log.info("Purchase receipt photo has been added for user {}", session.getChatId());
        endOfPurchaseReceipt(session, photoId != null
                ? PURCHASE_RECEIPT_ADDED_MESSAGE
                : PURCHASE_DETAILS_NO_ADDED_MESSAGE
        );
    }

    private void purchaseTransaction(UserSession session, String purchaseString, boolean isQuick) {
        log.info("Transaction of any type of purchase has been initiated for user {}", session.getChatId());
        updateMessageService.updateKeyboard(
                session.getChatId(), session.getLastMessageId(), null);
        String[] transactionData = purchaseString.split(SEPARATOR);
        Pattern pattern = Pattern.compile(MONEY_TRANSACTION_PATTERN);

        try {
            if (transactionData.length == 1) {
                ///for_spent one_currency
                purchaseForSpentOneCurrency(session, purchaseString, pattern);
            } else if (transactionData.length == 2) {
                ////for_spent two_currencies
                purchaseForSpentTwoCurrencies(session, pattern, transactionData);
            } else {
                throw new Exception();
            }

            if (isQuick) {
                log.info("Quick purchase completed for user {}", session.getChatId());
                endOfPurchase(session, PURCHASE_DETAILS_NO_ADDED_MESSAGE);
            } else {
                log.info("Moving on to the purchase description for user {}", session.getChatId());
                purchaseSettingsNextStepDescription(session);
            }

        } catch (NegativeAccountException e) {
            log.warn("Family {} has negative account", session.getFamilyId());
            sendMessageService.sendMessage(session.getChatId(),
                    EXCEPTION_NEGATIVE_ACCOUNT_MESSAGE, session.getLocale());
            toMain(session);
        } catch (CurrencyNotExistException e) {
            log.warn("Family {} has no such currency for purchase", session.getFamilyId());
            sendMessageService.sendMessage(session.getChatId(),
                    EXCEPTION_CURRENCY_NOT_EXIST_MESSAGE, session.getLocale());
            sendTryAgainMessage(PURCHASE_LOG_EXCEPTION, e, session, PURCHASE_NO_ADDED_MESSAGE);
        } catch (AmountException e) {
            log.warn("The entered purchase numeric data is incorrect from user {}", session.getChatId());
            sendMessageService.sendMessage(session.getChatId(),
                    EXCEPTION_AMOUNT_LESS_MESSAGE, session.getLocale());
            sendTryAgainMessage(PURCHASE_LOG_EXCEPTION, e, session, PURCHASE_NO_ADDED_MESSAGE);
        } catch (AccountNotExistException e) {
            log.warn("Family {} has no such account", session.getFamilyId());
            sendMessageService.sendMessage(session.getChatId(),
                    EXCEPTION_ACCOUNT_NOT_EXIST_MESSAGE, session.getLocale());
            sendTryAgainMessage(PURCHASE_LOG_EXCEPTION, e, session, PURCHASE_NO_ADDED_MESSAGE);
        } catch (Exception e) {
            log.warn("The entered purchase data is invalid from user {}", session.getChatId());
            sendTryAgainMessage(PURCHASE_LOG_EXCEPTION, e, session, PURCHASE_NO_ADDED_MESSAGE);
        }
    }

    private void purchaseSettingsNextStepDescription(UserSession session) {
        session.userIsBusy();
        sendMessageService.sendMessage(
                session.getChatId(), PURCHASE_ADD_DESCRIPTION_WRITE__FORMAT_MESSAGE,
                List.of(MAX_DESCRIPTION_LENGTH.toString()), session.getLocale(),
                inlineCommandHelper
                        .buildInlineKeyboard(session.getLocale(), SKIP_PURCHASE_DETAILS_BUTTON)
        );
        sendMessageService.sendMessageNotSaveMessage(session.getChatId(),
                PURCHASE_ADD_DESCRIPTION_REPLY, session.getLocale(),
                replyKeyboardHelper.buildKeyboardTwoPerLine(session.getLocale(), SIMPLE_DESCRIPTION_OPTIONS)
        );
    }

    private void toMain(UserSession userSession) {
        log.info("User {} redirect to main", userSession.getChatId());
        userSession.getPath().clear();
        sendMessageService.sendMessage(userSession.getChatId(),
                MAIN_MENU_MESSAGE, userSession.getLocale(),
                replyKeyboardHelper.buildKeyboardTwoPerLine(userSession.getLocale(),
                        List.of(
                                MAIN_QUICK_PURCHASE_REPLY, MAIN_INCOME_REPLY,
                                MAIN_PURCHASE_REPLY, MAIN_PROFILE_REPLY
                        )
                )
        );
    }

    private void setPurchaseDescription(UserSession session, String description) {
        updateMessageService.updateKeyboard(
                session.getChatId(), session.getLastMessageId(), null);
        String updatedDescription = trimDescriptionToLength(description);
        log.debug("Try to set purchase description {} for family {}", updatedDescription, session.getFamilyId());
        int longer = description.length() - updatedDescription.length();
        if (longer != 0) {
            log.info("Purchase description has been trimmed for user {}", session.getChatId());
            sendMessageService.sendMessage(
                    session.getChatId(), PURCHASE_DESCRIPTION_LONGER__FORMAT_MESSAGE,
                    List.of(Integer.toString(longer)), session.getLocale()
            );
        }
        session.getTransactionSession().getPurchaseCreation().addDescription(updatedDescription,
                SIMPLE_DESCRIPTION_OPTIONS.contains(updatedDescription));

        log.info("Purchase description has been added for user {}", session.getChatId());
        endOfPurchaseDescription(session, PURCHASE_DESCRIPTION_ADDED_MESSAGE);
    }

    private void endOfPurchaseDescription(UserSession session, String status) {
        sendMessageService.sendMessage(session.getChatId(), status,
                session.getLocale(), true);
        sendMessageService.sendMessage(session.getChatId(),
                PURCHASE_ADD_RECEIPT_MESSAGE, session.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(
                        session.getLocale(), SKIP_PURCHASE_DETAILS_BUTTON
                )
        );
    }

    private void endOfPurchaseReceipt(UserSession session, String status) {
        session.userIsFree();
        endOfPurchase(session, status);
    }

    private void endOfPurchase(UserSession session, String status) {
        try {
            PurchaseCreation purchaseCreation = session.getTransactionSession().getPurchaseCreation();
            takeMoney(session.getFamilyId(),
                    purchaseCreation.getAmount(), purchaseCreation.getCurrency());
            if (purchaseCreation.getSecondAmount() != null) {
                accountService.addMoneyToAccount(purchaseCreation.getSecondAmount(),
                        purchaseCreation.getSecondCurrency().getUsed(), session.getFamilyId());
                saveIncome(session, "change",
                        purchaseCreation.getSecondAmount(), purchaseCreation.getSecondCurrency());
            }
            Purchase purchase = new Purchase(
                    purchaseCreation,
                    session.getFamilyId(),
                    session.getChatId()
            );

            log.debug("Save purchase {} for family {}", purchase, session.getFamilyId());
            purchaseRepository.save(purchase);
            sendMessageService.sendMessage(session.getChatId(), status, session.getLocale());
            sendMessageService.sendMessage(session.getChatId(),
                    PURCHASE_ADDED_MESSAGE, session.getLocale());
        } catch (NegativeAccountException e) {
            log.warn("Family {} has negative account", session.getFamilyId());
            sendMessageService.sendMessage(session.getChatId(),
                    EXCEPTION_NEGATIVE_ACCOUNT_MESSAGE, session.getLocale());
            toMain(session);
        } catch (AccountNotExistException e) {
            log.warn("Family {} has no such account", session.getFamilyId());
            sendMessageService.sendMessage(session.getChatId(),
                    EXCEPTION_ACCOUNT_NOT_EXIST_MESSAGE, session.getLocale());
            sendTryAgainMessage(PURCHASE_LOG_EXCEPTION, e, session, PURCHASE_NO_ADDED_MESSAGE);
        } catch (Exception e) {
            log.warn("The entered purchase data is invalid from user {}", session.getChatId());
            sendTryAgainMessage(PURCHASE_LOG_EXCEPTION, e, session, PURCHASE_NO_ADDED_MESSAGE);
        }
        session.getTransactionSession().clearPurchaseCreation();
        endOfTransaction(session);
    }

    private void incomeTransaction(UserSession session, String incomeString) {
        log.info("Transaction of income has been initiated for user {}", session.getChatId());
        updateMessageService.deleteMessage(session.getChatId(), session.getLastMessageId());
        Pattern pattern = Pattern.compile(MONEY_TRANSACTION_DESCRIPTION_PATTERN);
        Matcher matcher = pattern.matcher(incomeString.trim());

        try {
            workWithIncomeTransaction(session, matcher);
        } catch (CurrencyNotExistException e) {
            log.warn("Family {} has no such currency for income", session.getFamilyId());
            sendMessageService.sendMessage(session.getChatId(),
                    EXCEPTION_CURRENCY_NOT_EXIST_MESSAGE, session.getLocale());
            sendTryAgainMessage(INCOME_LOG_EXCEPTION, e, session, INCOME_NO_ADDED_MESSAGE);
        } catch (Exception e) {
            log.warn("The entered income data is invalid from user {}", session.getChatId());
            sendTryAgainMessage(INCOME_LOG_EXCEPTION, e, session, INCOME_NO_ADDED_MESSAGE);
        }
    }

    private void sendTryAgainMessage(String logMessage, Exception e,
                                     UserSession session, String noAddedMessage) {
        log.warn(logMessage, e.getMessage());
        sendMessageService.sendMessage(session.getChatId(),
                noAddedMessage, session.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                        List.of(RULE_BUTTON, BACK_REMOVE_BUTTON)));
    }

    private void purchaseForSpentOneCurrency(
            UserSession session, String purchaseString, Pattern pattern
    ) throws Exception {
        Matcher matcher = pattern.matcher(purchaseString.trim());
        if (!matcher.matches()) {
            log.debug("Failed to parse purchase string for one currency: '{}', userId: {}",
                    purchaseString, session.getChatId());
            throw new Exception();
        }

        double amount = Double.parseDouble(matcher.group(1).replace(COMA, POINT));
        Currency currency = getCurrency(matcher.group(3), getFamilyCurrencies(session.getFamilyId()));
        if (currency == null) {
            log.debug("Currency not found for '{}', userId: {}", matcher.group(3), session.getChatId());
            throw new CurrencyNotExistException();
        }
        accountService.checkAccount(currency, session.getFamilyId());

        log.info("Processing one-currency purchase. Amount: {}, Currency: {}, userId: {}",
                amount, currency.getUsed(), session.getChatId());
        session.getTransactionSession().setPurchaseCreation(new PurchaseCreation(amount, currency));
    }

    private void purchaseForSpentTwoCurrencies(
            UserSession session, Pattern pattern, String[] transactionData
    ) throws Exception {
        Matcher matcher1 = pattern.matcher(transactionData[0].trim());
        Matcher matcher2 = pattern.matcher(transactionData[1].trim());

        if (!matcher1.matches() || !matcher2.matches()) {
            log.debug("Failed to parse one or both transactions for two-currency purchase. Data: [{} | {}], userId: {}",
                    transactionData[0], transactionData[1], session.getChatId());
            throw new Exception();
        }

        double amount1 = Double.parseDouble(matcher1.group(1).replace(COMA, POINT));
        double amount2 = Double.parseDouble(matcher2.group(1).replace(COMA, POINT));

        Set<String> familyCurrencies = getFamilyCurrencies(session.getFamilyId());
        Currency currency1 = getCurrency(matcher1.group(3), familyCurrencies);
        Currency currency2 = getCurrency(matcher2.group(3), familyCurrencies);
        if (currency1 == null || currency2 == null) {
            log.debug("Currency not found for two-currency purchase. currency1: '{}', currency2: '{}', userId: {}",
                    matcher1.group(3), matcher2.group(3), session.getChatId());
            throw new CurrencyNotExistException();
        }
        accountService.checkAccount(currency1, session.getFamilyId());

        double changeAmount = currency1.equals(currency2) ? amount1
                : exchangeRateService.getConvertCurrency(amount1, currency1.getCode(), currency2.getCode());
        changeAmount -= amount2;

        if (changeAmount < 0) {
            log.debug("Change after currency conversion is negative. userId: {}, change: {}",
                    session.getChatId(), changeAmount);
            throw new AmountException();
        }

        log.info("Processing two-currency purchase. amount1: {}, currency1: {}, amount2: {}, currency2: {}, change: {}, userId: {}",
                amount1, currency1.getUsed(), amount2, currency2.getUsed(), changeAmount, session.getChatId());
        session.getTransactionSession().setPurchaseCreation(
                new PurchaseCreation(amount1, currency1, changeAmount, currency2));
    }

    private void workWithIncomeTransaction(UserSession session, Matcher matcher) throws Exception {
        if (!matcher.matches()) {
            log.debug("Failed to parse income transaction string, userId: {}", session.getChatId());
            throw new Exception();
        }

        double amount = Double.parseDouble(matcher.group(1).replace(COMA, POINT));
        Currency currency = getCurrency(matcher.group(3), getFamilyCurrencies(session.getFamilyId()));
        if (currency == null) {
            log.debug("Currency not found for income transaction: '{}', userId: {}",
                    matcher.group(3), session.getChatId());
            throw new CurrencyNotExistException();
        }
        String description = matcher.group(6);

        double newAmountOfAccount = getNewAmountOfAccount(session, description, amount, currency);
        log.info("Income transaction processed. Amount: {}, Currency: {}, Description: '{}', New Account Balance: {}, userId: {}",
                amount, currency.getUsed(), description, newAmountOfAccount, session.getChatId());

        sendMessageService.sendMessage(
                session.getChatId(),
                INCOME_ADDED_MESSAGE,
                List.of(String.format(AMOUNT_FORMAT, newAmountOfAccount), currency.getUsed()),
                session.getLocale()
        );
        endOfTransaction(session);
    }


    private double getNewAmountOfAccount(UserSession session, String description, double amount, Currency currency) {
        log.debug("Save income {}{} for family {}", amount, currency, session.getFamilyId());
        saveIncome(session, description, new BigDecimal(amount), currency);

        return accountService.addMoneyToAccount(
                amount, currency, session.getFamilyId());
    }

    private void saveIncome(UserSession session, String description, BigDecimal amount, Currency currency) {
        Income income;
        if (description == null || description.isEmpty()) {
            income = new Income(amount, currency.getUsed(), session);
        } else {
            income = new Income(amount, currency.getUsed(),
                    trimDescriptionToLength(description), session);
        }
        incomeRepository.save(income);
    }

    private void takeMoney(Long familyId, BigDecimal amount, Currency currency)
            throws AccountNotExistException, NegativeArraySizeException {
        double newAmountOfFirstCurrencyAccount =
                accountService.takeMoneyFromAccount(
                        amount.doubleValue(), currency, familyId);
        if (newAmountOfFirstCurrencyAccount < 0) {
            log.warn("family{} has money less than 0", familyId);
            callAllFamily(familyId,
                    List.of(currency.getFullName()));
        }
    }

    private void callAllFamily(Long familyId, List<String> param) {
        log.info("Send message to all family members about negative account");
        List<UserData> users = userDataRepository.findAllUsersByFamilyId(familyId);
        sendMessageService
                .sendMessageForSomeUsersNotSaveMessage(
                        users, PURCHASE_NEGATIVE_ACCOUNT__FORMAT_MESSAGE, param
                );
    }

    private Currency getCurrency(String currencyString, Set<String> familyCurrencies) {
        for (Currency currency : Currency.values()) {
            if (!familyCurrencies.contains(currency.getCode())) {
                continue;
            }
            for (String s : currency.getCurrencyDescription()) {
                if (s.equalsIgnoreCase(currencyString)) {
                    return currency;
                }
            }
        }
        return null;
    }

    private void sendWaitingMessage(UserSession session) {
        sendMessageService.sendTemporaryMessage(session.getChatId(),
                TEMPORARY_MESSAGE_WAIT_MESSAGE, session.getLocale()
        );
    }

    private void endOfTransaction(UserSession session) {
        log.info("End of transaction for user {}", session.getChatId());
        toMain(session);
    }

    private String trimDescriptionToLength(String description) {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        } else {
            return description.substring(0, MAX_DESCRIPTION_LENGTH);
        }
    }

    private Set<String> getFamilyCurrencies(Long familyId) {
        return Arrays.stream(familyDataRepository
                        .findFamilyCurrenciesById(familyId).split(COMA))
                .collect(Collectors.toSet());
    }
}
