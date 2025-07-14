package org.example.service.support.special;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.model.session.UserSession;
import org.example.sender.service.SendMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.example.bot.Dispatcher.USER_SESSION_MAP;
import static org.example.util.MessageProperties.TRANSACTION_CANCELED_MESSAGE;

@Slf4j
@Component
public class ShutdownTransactionCleaner {

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
                    sendMessageService.sendMessage(userSession.getChatId(),
                            TRANSACTION_CANCELED_MESSAGE, userSession.getLocale());
                }
            }
        }
        log.info("Canceling all pending transactions is complete.");
    }
}

