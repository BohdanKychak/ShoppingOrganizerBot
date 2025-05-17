package org.example.service.support.special;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseCreation;
import org.example.sender.service.SendMessageService;
import org.example.service.response.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

import static org.example.bot.Dispatcher.USER_SESSION_MAP;
import static org.example.util.MessageProperties.TRANSACTION_CANCELED_MESSAGE;

@Slf4j
@Component
public class ShutdownTransactionCleaner {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SendMessageService sendMessageService;

    // This method cancels all pending transactions before the application terminates.
    @PreDestroy
    public void onShutdown() {
        log.info("Start check UserSessions for transaction cancellation");
        for (Map.Entry<Long, UserSession> entry : USER_SESSION_MAP.entrySet()) {
            UserSession userSession = entry.getValue();
            if (userSession.isBusy()) {
                if (userSession.getTransactionSession().getPurchaseCreation() != null) {
                    transactionCancellation(userSession);
                }
            }
        }
        log.info("Canceling all pending transactions is complete.");
    }

    private void transactionCancellation(UserSession userSession) {
        PurchaseCreation purchaseCreation = userSession.getTransactionSession().getPurchaseCreation();
        BigDecimal amount = purchaseCreation.getAmount();
        String currency = purchaseCreation.getCurrency();
        userSession.getTransactionSession().clearPurchaseCreation();

        accountService.addMoneyToAccount(amount, currency, userSession.getFamilyId());
        sendMessageService.sendMessage(userSession.getChatId(),
                TRANSACTION_CANCELED_MESSAGE, userSession.getLocale());
    }
}

