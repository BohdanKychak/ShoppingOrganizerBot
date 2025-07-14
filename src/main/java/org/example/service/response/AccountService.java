package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.enums.Currency;
import org.example.exception.AccountNotExistException;
import org.example.exception.NegativeAccountException;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.entity.Account;
import org.example.model.session.UserSession;
import org.example.repository.AccountRepository;
import org.example.repository.FamilyDataRepository;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.example.action.tag.InlineButtonKey.ACCOUNT_BUTTON;
import static org.example.action.tag.InlineButtonKey.BACK_REMOVE_BUTTON;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.NEW_LINE;
import static org.example.util.Constants.ZERO;
import static org.example.util.MessageProperties.FAMILY_ACCOUNT_EMPTY_MESSAGE;
import static org.example.util.MessageProperties.FAMILY_ACCOUNT_INFO__FORMAT_MESSAGE;

@Slf4j
@Component
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FamilyDataRepository familyDataRepository;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    public void getFamilyAccountInfo(UserSession session, Update update) {
        log.info("Opening family account info for family {} for user {}",
                session.getFamilyId(), session.getChatId());
        updateMessageService.deleteMessage(session.getChatId(),
                update.getCallbackQuery().getMessage().getMessageId());
        session.getPath().push(ACCOUNT_BUTTON);

        String accountTable = getAccountTable(session.getLocale(),
                accountRepository.findAccountsByFamilyId(session.getFamilyId()),
                familyDataRepository
                        .findFamilyCurrenciesById(session.getFamilyId()));

        if (accountTable.isEmpty()) {
            log.debug("Family {} has no accounts", session.getFamilyId());
            sendMessageService.sendMessage(session.getChatId(),
                    FAMILY_ACCOUNT_EMPTY_MESSAGE, session.getLocale(),
                    inlineCommandHelper.buildInlineKeyboard(
                            session.getLocale(), BACK_REMOVE_BUTTON)
            );
        } else {
            log.debug("Family {} has accounts:\n{}", session.getFamilyId(), accountTable);
            sendMessageService.sendMessage(
                    session.getChatId(), accountTable,
                    inlineCommandHelper.buildInlineKeyboard(
                            session.getLocale(), List.of(BACK_REMOVE_BUTTON))
            );
        }
    }

    public void addMoneyToAccount(BigDecimal amount, String currency, Long familyId) {
        addMoneyToAccount(amount.doubleValue(), Currency.getCurrencyByStringUsed(currency), familyId);
    }

    public Double addMoneyToAccount(double amount, Currency currency, Long familyId) {
        Optional<Account> oAccount = accountRepository
                .findAccountByFamilyIdAndCurrency(familyId, currency.getUsed());
        Account account = oAccount
                .orElseGet(() -> checkAndCreateAccount(currency, familyId));

        double newAmount = account.getAccountAmount().doubleValue() + amount;
        accountRepository.updateAmountById(account.getAccountId(),
                new BigDecimal(newAmount).setScale(2, RoundingMode.HALF_UP));
        log.debug("Added {} {} to account {} for family {}",
                amount, currency.getCode(), account.getAccountId(), familyId);
        return newAmount;
    }

    public Account checkAccount(Currency currency, Long familyId)
            throws AccountNotExistException, NegativeAccountException {
        Optional<Account> oAccount = accountRepository
                .findAccountByFamilyIdAndCurrency(familyId, currency.getUsed());
        if (oAccount.isEmpty()) throw new AccountNotExistException();
        Account account = oAccount.get();
        if (account.getAccountAmount().compareTo(BigDecimal.ZERO) < ZERO) throw new NegativeAccountException();

        return account;
    }

    public double takeMoneyFromAccount(double amount, Currency currency, Long familyId)
            throws AccountNotExistException, NegativeAccountException {
        Account account = checkAccount(currency, familyId);

        double newAmount = account.getAccountAmount().doubleValue() - amount;
        accountRepository.updateAmountById(account.getAccountId(),
                new BigDecimal(newAmount).setScale(2, RoundingMode.HALF_UP));
        log.debug("Took {} {} from account {} for family {}",
                amount, currency.getCode(), account.getAccountId(), familyId);
        return newAmount;
    }

    private synchronized Account checkAndCreateAccount(Currency currency, Long familyId) {
        Optional<Account> oAccount = accountRepository
                .findAccountByFamilyIdAndCurrency(familyId, currency.getUsed());
        if (oAccount.isPresent()) {
            return oAccount.get();
        } else {
            Account account = new Account(currency, 0.0, familyId);
            accountRepository.save(account);
            log.info("created new account for {} family in {}", familyId, currency.getCode());
            return account;
        }
    }

    private static String getAccountTable(
            Locale locale, List<Account> accounts, String familyCurrencies) {
        String accountFormat =
                getMessage(FAMILY_ACCOUNT_INFO__FORMAT_MESSAGE, locale);
        StringBuilder message = new StringBuilder();
        for (Account account : accounts) {
            if (!familyCurrencies.contains(account.getCurrencyCode())) {
                continue;
            }
            message.append(
                    String.format(
                            accountFormat,
                            account.getCurrencyName(),
                            account.getAccountAmount(),
                            account.getCurrency()
                    )
            ).append(NEW_LINE);
        }
        return message.toString();
    }
}
