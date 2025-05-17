package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.enums.Currency;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.entity.FamilyData;
import org.example.model.session.UserSession;
import org.example.repository.FamilyDataRepository;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.example.service.support.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.example.action.tag.InlineButtonKey.*;
import static org.example.enums.Currency.getCurrenciesDescriptionList;
import static org.example.enums.Currency.getCurrencyCodes;
import static org.example.enums.Currency.getCurrencyCodesWithStatus;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.*;
import static org.example.util.MessageProperties.*;

@Slf4j
@Component
public class CurrencyService {

    private static final int NUMBER_AFTER_POINT = 2;

    @Autowired
    private FamilyDataRepository familyDataRepository;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Autowired
    private ExchangeRateService exchangeRateService;

    public boolean currencyCalculator(UserSession userSession, String receivedText) {
        log.debug("Check message {} in currencyCalculator tab", receivedText);
        if (receivedText != null
                && CURRENCY_EXCHANGE_RATE_TEST_BUTTON.equals(userSession.getPath().peek())) {

            deleteLastButtonMenu(userSession);

            String[] text = receivedText.split(SEPARATOR);
            if (text.length == 3) {
                if (calculate(userSession, text,
                        inlineCommandHelper.buildInlineKeyboard(
                                userSession.getLocale(), FAMILY_SETTINGS_BUTTON))) {
                    return true;
                }
            }
            log.info("User {} tried to calculate currency, but failed",
                    userSession.getChatId());
            sendMessageService.sendMessage(
                    userSession.getChatId(), EXCHANGE_RATE_TEST_WRONG_MESSAGE,
                    userSession.getLocale(),
                    inlineCommandHelper
                            .buildInlineKeyboard(userSession.getLocale(),
                                    List.of(RULE_BUTTON, FAMILY_SETTINGS_BUTTON))
            );
            return true;
        }
        return false;
    }

    public void changeFamilyCurrency(UserSession userSession, Integer messageId) {
        log.info("Open family currency settings for user {}", userSession.getChatId());
        updateMessageService.editInlineMessage(
                userSession.getChatId(), messageId,
                userSession.getLocale(), FAMILY_CURRENCY_WORK_MESSAGE
        );
        userSession.getPath().push(FAMILY_CHANGE_CURRENCY_BUTTON);
        List<String> activeCurrencies =
                new ArrayList<>(getFamilyData(userSession).getCurrency());
        log.debug("User {} tried to change family currency {}",
                userSession.getChatId(), activeCurrencies);

        sendMessageService.sendMessage(
                userSession.getChatId(),
                FAMILY_CURRENCY_SETTINGS__FORMAT_MESSAGE,
                List.of(ACTIVE, INACTIVE, getCurrenciesDescriptionList()),
                userSession.getLocale(),
                inlineCommandHelper.buildInlineKeyboardMaxThreeButton(
                        userSession.getLocale(),
                        getCurrencyCodesWithStatus(activeCurrencies),
                        getCurrencyCodes(),
                        CURRENCY_EXCHANGE_RATE_TABLE_BUTTON,
                        BACK_REMOVE_BUTTON
                )
        );
    }

    public void workWithCurrency(UserSession userSession, String currency, Integer messageId) {
        log.info("Update family currency for user {}", userSession.getChatId());
        FamilyData familyData = getFamilyData(userSession);
        List<String> currencies = new ArrayList<>(familyData.getCurrency());
        if (currencies.contains(currency)) {
            currencies.remove(currency);
        } else {
            currencies.add(currency);
        }
        familyData.setCurrency(new HashSet<>(currencies));
        familyDataRepository.save(familyData);
        updateMessageService.updateKeyboard(userSession.getChatId(), messageId,
                inlineCommandHelper.buildInlineKeyboardMaxThreeButton(
                        userSession.getLocale(),
                        getCurrencyCodesWithStatus(new ArrayList<>(familyData.getCurrency())),
                        getCurrencyCodes(),
                        CURRENCY_EXCHANGE_RATE_TABLE_BUTTON,
                        BACK_REMOVE_BUTTON
                )
        );

    }

    public void workWithExchangeRate(UserSession userSession, Integer messageId) {
        log.info("Open exchange rate table for user {}", userSession.getChatId());
        userSession.getPath().push(CURRENCY_EXCHANGE_RATE_TABLE_BUTTON);
        Set<String> familyCurrencies = getFamilyData(userSession).getCurrency();
        updateMessageService.editInlineMessage(
                userSession.getChatId(),
                messageId,
                null,
                String.format(
                        getMessage(FAMILY_CURRENT_CURRENCY__FORMAT_MESSAGE, userSession.getLocale()),
                        String.join(COMMA_SEPARATED_FORMAT, familyCurrencies))
        );
        if (familyCurrencies.size() > 1) {
            String exchangeRateTable = exchangeRateService
                    .getExchangeRatesTable(familyCurrencies);
            if (exchangeRateTable != null) {
                sendMessageService.sendMessage(
                        userSession.getChatId(),
                        EXCHANGE_RATE_TABLE__FORMAT_MESSAGE,
                        List.of(exchangeRateTable),
                        userSession.getLocale(),
                        inlineCommandHelper.buildInlineKeyboard(
                                userSession.getLocale(),
                                List.of(
                                        CURRENCY_EXCHANGE_RATE_TEST_BUTTON,
                                        BACK_REMOVE_BUTTON
                                )
                        )
                );
                return;
            }
        }
        sendMessageService.sendMessage(userSession.getChatId(),
                EXCHANGE_RATE_NOT_AVAILABLE_MESSAGE, userSession.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(
                        userSession.getLocale(),
                        BACK_REMOVE_BUTTON)
        );
    }

    public void turnOnExchangeRateTest(UserSession userSession, Integer messageId) {
        log.info("Open exchange rate test for user {}", userSession.getChatId());
        updateMessageService.updateKeyboard(userSession.getChatId(), messageId, null);
        userSession.getPath().push(CURRENCY_EXCHANGE_RATE_TEST_BUTTON);
        sendMessageService.sendMessage(
                userSession.getChatId(),
                EXCHANGE_RATE_TEST_MESSAGE,
                userSession.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(
                        userSession.getLocale(),
                        List.of(RULE_BUTTON, FAMILY_SETTINGS_BUTTON)
                )
        );
    }

    private void deleteLastButtonMenu(UserSession userSession) {
        if (userSession.getLastMessageId() != null) {
            updateMessageService.updateKeyboard(
                    userSession.getChatId(),
                    userSession.getLastMessageId(),
                    null
            );
        }
    }

    private boolean calculate(
            UserSession userSession, String[] text, ReplyKeyboard keyboardMarkup) {
        try {
            Currency fromCurrency = getCurrencyByString(text[0].trim());
            double amount = getAmountBySting(text[1].trim());
            Currency toCurrency = getCurrencyByString(text[2].trim());

            if (checkCurrencyCalculatorValues(fromCurrency, amount, toCurrency)) {
                throw new Exception("Error parsing values");
            }

            double result = exchangeRateService.getConvertCurrency(
                    amount, fromCurrency.getCode(), toCurrency.getCode());
            log.info("User {} successfully calculated currency",
                    userSession.getChatId());
            sendMessageService.sendMessage(
                    userSession.getChatId(),
                    String.format(CURRENCY_CONVERT__FORMAT,
                            amount, fromCurrency.getUsed(),
                            result, toCurrency.getUsed()),
                    keyboardMarkup
            );
            return true;
        } catch (Exception e) {
            log.error("Error parsing values: {}", e.getMessage());
        }
        return false;
    }

    private FamilyData getFamilyData(UserSession userSession) {
        return familyDataRepository.findFamilyById(userSession.getFamilyId());
    }

    private static double getAmountBySting(String amount) {
        BigDecimal number = new BigDecimal(amount.replace(COMA, POINT));
        return number.setScale(NUMBER_AFTER_POINT, RoundingMode.HALF_UP).doubleValue();
    }

    private static Currency getCurrencyByString(String currencyString) {
        for (Currency currency : Currency.values()) {
            boolean contains = currency.getCurrencyDescription().stream()
                    .anyMatch(s -> s.equalsIgnoreCase(
                            currencyString.replaceAll(IGNORE_SYMBOL, EMPTY)));
            if (contains) {
                return currency;
            }
        }
        return null;
    }

    private boolean checkCurrencyCalculatorValues(
            Currency fromCurrency, double amount, Currency toCurrency) {
        return fromCurrency == null || toCurrency == null
                || amount < 0 || amount > MAX_AMOUNT_CURRENCY_TEST;
    }

}
