package org.example.service.response.transaction_info.purchase;

import lombok.extern.slf4j.Slf4j;
import org.example.enums.Currency;
import org.example.enums.FilterCriteria;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.helper.keyboard.ReplyKeyboardHelper;
import org.example.model.entity.Purchase;
import org.example.model.session.TransactionSession;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseSortedHistory;
import org.example.repository.PurchaseRepository;
import org.example.repository.specifications.PurchaseSpecifications;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.example.action.tag.InlineButtonKey.*;
import static org.example.enums.Currency.getCurrencyCodes;
import static org.example.enums.Currency.getCurrencyCodesWithStatus;
import static org.example.enums.FilterCriteria.AMOUNT;
import static org.example.enums.FilterCriteria.CURRENCY;
import static org.example.enums.FilterCriteria.DATE;
import static org.example.enums.FilterCriteria.USER;
import static org.example.enums.FilterCriteria.getFilterCriteriaCodes;
import static org.example.repository.specifications.PurchaseSpecifications.getTimePurchaseDescSort;
import static org.example.service.support.PropertiesService.getInlineButton;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.*;
import static org.example.util.MessageProperties.*;

@Slf4j
@Component
public class PurchaseFilterService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchaseSortedService purchaseSortedService;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Autowired
    private ReplyKeyboardHelper replyKeyboardHelper;

    public void showFilterMenu(UserSession session) {
        log.info("Show filter menu for user {}", session.getChatId());
        Locale locale = session.getLocale();
        if (!PURCHASES_FILTER_BUTTON.equals(session.getPath().peek())) {
            session.getTransactionSession().clearPurchaseSortedHistory();
            session.getPath().push(PURCHASES_FILTER_BUTTON);
        }
        checkTempData(session);

        InlineKeyboardMarkup keyboardMarkup = getKeyboardFilterMenu(session, locale);

        updateMessageService.editInlineMessage(session.getChatId(),
                session.getLastMessageId(), locale, FILTER_MENU_MESSAGE, keyboardMarkup);
    }

    public boolean checkNonTypicalPurchaseFilterCommand(UserSession session, String receivedText) {
        log.debug("Check message {} in purchaseFilter tab", receivedText);
        return setAmountCriteria(session, receivedText)
                || setDateCriteria(session, receivedText);
    }

    public void userFilterSetupMenu(UserSession session) {
        log.debug("User filter setup menu for user {}", session.getChatId());
        session.getPath().push(USER.getCode());
        updateMessageService.editInlineMessage(session.getChatId(),
                session.getLastMessageId(), session.getLocale(), FILTER_MENU_USER_MESSAGE,
                inlineCommandHelper
                        .buildInlineKeyboardMaxThreeButton(session.getLocale(),
                                List.of(USER_FILTER_WITH_BUTTON,
                                        USER_FILTER_WITHOUT_BUTTON,
                                        USER_FILTER_ALL_BUTTON)
                        )
        );
    }

    public void setUserCriteria(UserSession session, Update update) {
        session.getPath().pop();
        Boolean newActiveStateUser = getActiveUserState(update.getCallbackQuery().getData());

        PurchaseSortedHistory history = session.getTransactionSession().getPurchaseSortedHistory();

        if (!objectEquals(newActiveStateUser, history.getActiveUserState())) {
            if (newActiveStateUser == null) {
                newKeyboard(history.getKeyboardMarkup().getKeyboard(), USER,
                        getInlineButton(
                                USER.getCode(), session.getLocale()
                        ) + INACTIVE);
            }
            if (history.getActiveUserState() == null) {
                newKeyboard(
                        history.getKeyboardMarkup().getKeyboard(), USER,
                        getInlineButton(
                                USER.getCode(), session.getLocale()
                        ) + ACTIVE);
            }
            log.info("User filter criteria changed for user {}", session.getChatId());
        }
        history.setActiveUserState(newActiveStateUser);

        showFilterMenu(session);
    }

    public void amountFilterSetupMenu(UserSession session) {
        log.debug("Amount filter setup menu for user {}", session.getChatId());
        session.getPath().push(AMOUNT.getCode());
        updateMessageService.editInlineMessage(session.getChatId(),
                session.getLastMessageId(), session.getLocale(), FILTER_MENU_AMOUNT_MESSAGE,
                inlineCommandHelper
                        .buildInlineKeyboardMaxThreeButton(session.getLocale(),
                                List.of(RULE_BUTTON, BACK_MODIFY_BUTTON)
                        )
        );
    }

    public boolean setAmountCriteria(UserSession session, String receivedText) {
        if (!AMOUNT.getCode().equals(session.getPath().peek())) {
            return false;
        }
        if (IsAmountCriteriaAdded(session, receivedText)) return true;
        log.info("Amount filter criteria changed for user {}", session.getChatId());

        session.getPath().pop();
        showFilterMenu(session);
        return true;
    }

    public void dateFilterSetupMenu(UserSession session) {
        log.debug("Date filter setup menu for user {}", session.getChatId());
        session.getPath().push(DATE.getCode());
        Message message = sendMessageService
                .sendMessageNotSaveMessage(session.getChatId(),
                        getMessage(FILTER_DATE_SIMPLIFICATION_BUTTONS, session.getLocale()),
                        replyKeyboardHelper.buildKeyboardTwoPerLine(null,
                                getSimplificationButtons()
                        )
                );
        session.setTemporaryData(message.getMessageId().toString());
        updateMessageService.editInlineMessage(session.getChatId(),
                session.getLastMessageId(), session.getLocale(), FILTER_MENU_DATE_MESSAGE,
                inlineCommandHelper
                        .buildInlineKeyboard(session.getLocale(),
                                List.of(RULE_BUTTON, BACK_MODIFY_BUTTON)
                        )
        );
    }

    public boolean setDateCriteria(UserSession session, String receivedText) {
        if (!DATE.getCode().equals(session.getPath().peek())) {
            return false;
        }
        if (IsDateCriteriaAdded(session, receivedText)) return true;
        log.info("Date filter criteria changed for user {}", session.getChatId());

        session.getPath().pop();
        showFilterMenu(session);
        return true;
    }

    public void currencyFilterSetupMenu(UserSession session) {
        log.debug("Currency filter setup menu for user {}", session.getChatId());
        session.getPath().push(CURRENCY.getCode());
        updateMessageService.editInlineMessage(session.getChatId(),
                session.getLastMessageId(), session.getLocale(), FILTER_MENU_CURRENCY_MESSAGE,
                inlineCommandHelper.buildInlineKeyboardMaxThreeButton(
                        session.getLocale(),
                        getCurrencyCodesWithStatus(session.getTransactionSession().getPurchaseSortedHistory().getCurrencies()),
                        getCurrencyCodes(),
                        RULE_BUTTON,
                        READY_BUTTON
                )
        );
    }

    public void processCurrencySetup(UserSession session, Update update) {
        List<String> currencies = getNewCurrenciesCriteriaList(session, update);
        log.debug("Process currency setup for user {}", session.getChatId());
        updateMessageService.updateKeyboard(session.getChatId(),
                session.getLastMessageId(),
                inlineCommandHelper.buildInlineKeyboardMaxThreeButton(
                        session.getLocale(),
                        getCurrencyCodesWithStatus(currencies),
                        getCurrencyCodes(),
                        RULE_BUTTON,
                        READY_BUTTON
                )
        );
    }

    public void setCurrencyCriteria(UserSession session) {
        session.getPath().pop();
        PurchaseSortedHistory history = session.getTransactionSession().getPurchaseSortedHistory();
        if (history.getCurrencies() == null || history.getCurrencies().isEmpty()) {
            log.info("Currency filter criteria changed for user {}", session.getChatId());
            newKeyboard(history.getKeyboardMarkup().getKeyboard(), CURRENCY,
                    getInlineButton(
                            CURRENCY.getCode(), session.getLocale()
                    ) + INACTIVE);
        } else {
            log.info("Currency filter criteria devastated for user {}", session.getChatId());
            newKeyboard(history.getKeyboardMarkup().getKeyboard(), CURRENCY,
                    getInlineButton(
                            CURRENCY.getCode(), session.getLocale()
                    ) + ACTIVE);
        }

        showFilterMenu(session);
    }

    public void generateTable(UserSession session) {
        log.info("Start to generate table for user {}", session.getChatId());
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);
        if (!isReadyToFilter(session)) {
            log.info("User {} try to generate empty filter", session.getChatId());
            emptyFilter(session);
            return;
        }
        session.getPath().push(PURCHASE_GENERATE_PATH_CODE);
        updateMessageService.editInlineMessage(session.getChatId(), session.getLastMessageId(),
                session.getLocale(), GENERATE_TABLE_PROCESS_MESSAGE, null);

        TransactionSession transactionSession = session.getTransactionSession();
        List<Purchase> purchases = getPurchases(session, transactionSession.getPurchaseSortedHistory());
        transactionSession.setPurchaseSortedHistory(new PurchaseSortedHistory(purchases));

        log.info("The table generation process is completed for user {}", session.getChatId());
        purchaseSortedService.sendTable(session, true);
    }

    private void checkTempData(UserSession session) {
        if (session.getTemporaryData() != null) {
            try {
                updateMessageService.deleteMessage(session.getChatId(),
                        Integer.parseInt(session.getTemporaryData()));
                session.setTemporaryData(null);
            } catch (Exception e) {
                log.warn("Something wrong...{}", session.getTemporaryData());
            }
        }
    }

    private InlineKeyboardMarkup getKeyboardFilterMenu(UserSession session, Locale locale) {
        InlineKeyboardMarkup keyboardMarkup;
        if (session.getTransactionSession().getPurchaseSortedHistory() == null ||
                session.getTransactionSession().getPurchaseSortedHistory().getKeyboardMarkup() == null) {
            List<String> values = new ArrayList<>(getFilterCriteriaCodes());
            List<String> buttons = keyboardForEmptyCriteriaList(values, locale);
            values.addAll(List.of(RULE_BUTTON, BACK_MODIFY_BUTTON));
            buttons.addAll(List.of(getInlineButton(RULE_BUTTON, locale), getInlineButton(BACK_MODIFY_BUTTON, locale)));

            keyboardMarkup = inlineCommandHelper.buildInlineKeyboardWithoutLocale(buttons, values);
            session.getTransactionSession()
                    .setPurchaseSortedHistory(new PurchaseSortedHistory(keyboardMarkup));
        } else {
            keyboardMarkup = session.getTransactionSession().getPurchaseSortedHistory().getKeyboardMarkup();
        }
        return keyboardMarkup;
    }

    private ArrayList<String> keyboardForEmptyCriteriaList(List<String> keys, Locale locale) {
        return new ArrayList<>(keys.stream()
                .map(k -> getInlineButton(k, locale) + INACTIVE).toList());
    }

    private boolean objectEquals(Object o1, Object o2) {
        return (o1 == null && o2 == null) || (o1 != null && o1.equals(o2));
    }

    private void newKeyboard(List<List<InlineKeyboardButton>> keyboard,
                             FilterCriteria filterCriteria, String newButtonName) {
        int line = filterCriteria.getLine();
        int column = filterCriteria.getColumn();

        if (keyboard.size() > line && keyboard.get(line).size() > column) {
            keyboard.get(line).get(column).setText(newButtonName);
        }

    }

    private Boolean getActiveUserState(String choice) {
        if (USER_FILTER_WITH_BUTTON.equals(choice)) {
            return true;
        }
        if (USER_FILTER_WITHOUT_BUTTON.equals(choice)) {
            return false;
        }
        return null;
    }

    private boolean IsAmountCriteriaAdded(UserSession session, String receivedText) {
        String[] amounts = receivedText.split(SEPARATOR);

        try {
            PurchaseSortedHistory history = session.getTransactionSession().getPurchaseSortedHistory();
            tryToAddAmount(amounts, history, session.getLocale());
        } catch (Exception e) {
            incorrectData(session);
            return true;
        }
        return false;
    }

    private void tryToAddAmount(String[] amounts, PurchaseSortedHistory history, Locale locale) {
        if (amounts.length == 1) {
            history.setAmount1(new BigDecimal(amounts[0].trim()));
            history.setAmount2(null);
        } else if (amounts.length == 2) {
            history.setAmount1(new BigDecimal(amounts[0].trim()));
            history.setAmount2(new BigDecimal(amounts[1].trim()));

        } else {
            throw new NumberFormatException();
        }
        newKeyboard(
                history.getKeyboardMarkup().getKeyboard(), AMOUNT,
                getInlineButton(
                        AMOUNT.getCode(), locale
                ) + ACTIVE);
    }

    private static List<String> getSimplificationButtons() {
        List<String> simplificationButtons = new ArrayList<>();
        simplificationButtons.add(LocalDateTime.now().format(ONE_DAY_DATE_FORMATTER));
        simplificationButtons.add(LocalDateTime.now().format(ONE_MONTH_DATE_FORMATTER));
        return simplificationButtons;
    }

    private boolean IsDateCriteriaAdded(UserSession session, String receivedText) {
        String[] dates = receivedText.split(SEPARATOR);

        try {
            PurchaseSortedHistory history = session.getTransactionSession().getPurchaseSortedHistory();
            tryToAddDate(dates, history, session.getLocale());
        } catch (Exception e) {
            incorrectData(session);
            return true;
        }
        return false;
    }

    private void tryToAddDate(String[] dates, PurchaseSortedHistory history, Locale locale) {
        dateFilterSetup(dates, history);
        newKeyboard(
                history.getKeyboardMarkup().getKeyboard(), DATE,
                getInlineButton(
                        DATE.getCode(), locale
                ) + ACTIVE);
    }

    private static void dateFilterSetup(String[] dates, PurchaseSortedHistory history) {
        if (dates.length == 1) {
            oneDateSetup(new ArrayList<>(List.of(dates[0].split(SEPARATOR3))), history);
        } else if (dates.length == 2) {
            twoDateSetup(dates, history);
        } else {
            throw new NumberFormatException();
        }
    }

    private static void oneDateSetup(List<String> dateInfo, PurchaseSortedHistory history) {
        if (dateInfo.size() == 3) {
            // 1 day
            LocalDateTime date = getLocalDateTime(dateInfo);
            history.setStart(date);
            history.setEnd(date.plusDays(1));
            return;
        }
        if (dateInfo.size() == 2) {
            // 1 month or 1 day without year
            if (dateInfo.get(1).trim().length() <= 2) {
                // 1 day
                addYear(dateInfo);
                oneDateSetup(dateInfo, history);
                return;
            }
            // 1 month
            LocalDateTime date = getLocalDateTime(dateInfo);
            history.setStart(date);
            history.setEnd(date.plusMonths(1));
            return;
        }
        if (dateInfo.size() == 1) {
            // 1 month without year
            addYear(dateInfo);
            oneDateSetup(dateInfo, history);
            return;
        }
        throw new NumberFormatException();
    }

    private static void twoDateSetup(String[] dates, PurchaseSortedHistory history) {
        LocalDateTime firstDate = getLocalDateTime(processDate(dates[0]));
        LocalDateTime secondDate = getLocalDateTime(processDate(dates[1]));
        if (firstDate.isAfter(secondDate)) {
            firstDate = firstDate.minusYears(1);
        }
        history.setStart(firstDate);
        history.setEnd(secondDate.plusDays(1));
    }

    private static List<String> processDate(String date) {
        List<String> dateList = new ArrayList<>(List.of(date.split(SEPARATOR3)));
        if (dateList.size() == 2) {
            addYear(dateList);
        }
        return dateList;
    }

    private static void addYear(List<String> date) {
        date.add(String.valueOf(LocalDateTime.now().getYear()));
    }

    private static LocalDateTime getLocalDateTime(List<String> dateInfo) {
        int index = 0;

        int day = dateInfo.size() == 3 ? Integer.parseInt(dateInfo.get(index++).trim()) : 1;
        int month = Integer.parseInt(dateInfo.get(index++).trim());
        int year = Integer.parseInt(dateInfo.get(index).trim());

        return LocalDateTime.of(year, month, day, 0, 0);
    }

    private List<String> getNewCurrenciesCriteriaList(UserSession session, Update update) {
        List<String> currencies = session.getTransactionSession().getPurchaseSortedHistory().getCurrencies();
        String currency = update.getCallbackQuery().getData();

        if (currencies == null || !currencies.contains(currency)) {
            if (currencies == null) {
                currencies = new ArrayList<>();
            }
            currencies.add(currency);
        } else {
            currencies.remove(currency);
        }
        setupCurrenciesCriteria(session, currencies);
        return currencies;
    }

    private void setupCurrenciesCriteria(UserSession session, List<String> currencies) {
        boolean isExist = currencies != null;
        if (isExist && currencies.size() >= Currency.values().length) {
            currencies.clear();
        }
        session.getTransactionSession().getPurchaseSortedHistory().setCurrencies(isExist ? currencies : new ArrayList<>());
    }

    private void incorrectData(UserSession session) {
        sendMessageService.sendMessageNotSaveMessage(session.getChatId(),
                ENTERED_DATA_INCORRECT, session.getLocale());
    }

    private boolean isReadyToFilter(UserSession session) {
        PurchaseSortedHistory filter = session.getTransactionSession().getPurchaseSortedHistory();
        return filter.getActiveUserState() != null
                || filter.getAmount1() != null
                || filter.getStart() != null
                || filter.getCurrencies() != null;
    }

    private void emptyFilter(UserSession session) {
        sendMessageService.sendMessage(
                session.getChatId(), FILTER_CRITERIA_NO_SET, session.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                        List.of(PURCHASES_INFO_BUTTON, PURCHASES_FILTER_BUTTON))
        );
    }

    private List<Purchase> getPurchases(UserSession session, PurchaseSortedHistory data) {
        Specification<Purchase> specification = Specification.where(
                        PurchaseSpecifications.familyIdEqual(session.getFamilyId()))
                .and(PurchaseSpecifications
                        .userChatIdOrDescription(session.getChatId(), DESCRIPTION_GIFT));

        if (data.getStart() != null && data.getEnd() != null) {
            specification = specification.and(PurchaseSpecifications
                    .timeBetween(data.getStart(), data.getEnd()));
        }
        if (data.getActiveUserState() != null) {
            if (data.getActiveUserState().equals(Boolean.TRUE)) {
                specification = specification.and(PurchaseSpecifications
                        .userChatIdEqual(session.getChatId()));
            } else {
                specification = specification.and(PurchaseSpecifications
                        .userChatIdNotEqual(session.getChatId()));
            }
        }
        if (data.getCurrencies() != null && !data.getCurrencies().isEmpty()) {
            specification = specification.and(PurchaseSpecifications
                    .currencyIn(getCurrencyUsedList(data.getCurrencies())));
        }
        if (data.getAmount1() != null) {
            if (data.getAmount2() != null) {
                specification = specification.and(PurchaseSpecifications
                        .amountBetween(data.getAmount1(), data.getAmount2()));
            } else {
                specification = specification.and(PurchaseSpecifications
                        .orderByClosestAmount(data.getAmount1()));
            }
        }
        PageRequest pageRequest = PageRequest.of(0, TRANSACTIONS_SORTED_TABLE_LIMIT, getTimePurchaseDescSort());

        List<Purchase> result = purchaseRepository.findAll(specification, pageRequest).getContent();

        if (data.getAmount1() != null) {
            return result.stream()
                    .sorted(Comparator
                            .comparingDouble((Purchase p) ->
                                    Math.abs(p.getAmount().doubleValue() - data.getAmount1().doubleValue()))
                            .thenComparing(Comparator.comparing(Purchase::getTimePurchase).reversed()))
                    .collect(Collectors.toList());
        }
        return result;
    }

    private List<String> getCurrencyUsedList(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(code -> Currency.getCurrencyByStringCode(code).getUsed())
                .collect(Collectors.toList());
    }

}
