package org.example.service.response.transaction_info.purchase;

import lombok.extern.slf4j.Slf4j;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.entity.Purchase;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseLastHistory;
import org.example.sender.service.AnswerMessageService;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.example.service.support.TransactionCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.example.action.tag.InlineButtonKey.*;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.PURCHASE_LAST_SPECIAL_CODE;
import static org.example.util.MessageProperties.PURCHASES_TABLE_EMPTY_MESSAGE;
import static org.example.util.MessageProperties.STOP_ANSWER_MESSAGE;
import static org.example.util.MessageProperties.TEMPORARY_MESSAGE_WAIT_MESSAGE;

@Slf4j
@Component
public class PurchaseLastService extends PurchaseInfoService {

    @Autowired
    private TransactionCacheService transactionCacheService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private AnswerMessageService answerMessageService;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Override
    public void showPurchaseTableInfo(UserSession session, Update update) {
        showPurchaseTableInfo(session, update.getCallbackQuery().getMessage().getMessageId());
    }

    @Override
    public void showPurchaseTableInfo(UserSession session, Integer messageId) {
        setUp(session);

        sendTable(transactionCacheService.getPurchasesTableData(session),
                session, messageId);
    }

    @Override
    public boolean openSelectedPurchaseWithoutMessage(UserSession session) {
        if (!PURCHASE_LAST_SPECIAL_CODE.equals(session.getPath().peek())) {
            return false;
        }
        sendMessageService.sendMessage(session.getChatId(),
                getMessage(TEMPORARY_MESSAGE_WAIT_MESSAGE, session.getLocale()));
        return openSelectedPurchase(session,
                session.getTransactionSession().getPurchaseLastHistory().getPurchase().getId());
    }

    @Override
    public boolean openSelectedPurchase(UserSession session, Long purchaseId) {
        PurchaseLastHistory purchaseLastHistory = transactionCacheService
                .getPurchaseById(session.getFamilyId(), purchaseId);

        if (purchaseLastHistory == null) {
            return false;
        }
        session.getTransactionSession().setPurchaseLastHistory(purchaseLastHistory);

        session.getPath().push(PURCHASE_LAST_SPECIAL_CODE);
        return workWithPurchaseLast(session);
    }

    @Override
    public void showReceipt(UserSession session, Update update) {
        Purchase purchase = session.getTransactionSession().getPurchaseLastHistory().getPurchase();
        showReceipt(session, purchase, update.getCallbackQuery().getId());
    }

    @Override
    public void receiptUpdateRequest(UserSession session, Update update, String receivedButton) {
        Purchase purchase = session.getTransactionSession().getPurchaseLastHistory().getPurchase();
        super.receiptUpdateRequest(session, update, purchase, receivedButton);
    }

    @Override
    public void receiptUpdateResult(UserSession session, String photo) {
        String lastCommand = session.getPath().pop();
        transactionCacheService.setPhoto(session, photo);
        sendMessageService.sendMessage(session.getChatId(),
                getAnswerForNewReceipt(lastCommand, photo), session.getLocale());
        openSelectedPurchaseWithoutMessage(session);
    }

    @Override
    public void descriptionUpdateRequest(UserSession session, Update update) {
        Purchase purchase = session.getTransactionSession().getPurchaseLastHistory().getPurchase();
        super.descriptionUpdateRequest(session, update, purchase);
    }

    @Override
    public boolean descriptionUpdateResult(UserSession session, String description) {
        log.debug("Check message {} in descriptionUpdate purchaseLast tab", description);
        if (!PURCHASE_DESCRIPTION_BUTTON.equals(session.getPath().peek())
                || !session.getPath().contains(PURCHASE_LAST_SPECIAL_CODE)) {
            return false;
        }
        session.getPath().pop();
        String oldDescription = session.getTransactionSession().getPurchaseLastHistory().getPurchase().getDescription();
        transactionCacheService.setDescription(session, trimDescriptionToMaxLength(description));

        updateMessageService.deleteMessage(session.getChatId(), session.getLastMessageId());
        sendMessageService.sendMessage(session.getChatId(),
                getAnswerForNewDescription(oldDescription), session.getLocale());
        openSelectedPurchaseWithoutMessage(session);
        return true;
    }

    @Override
    public void deletePurchaseRequest(UserSession session, Update update) {
        Purchase purchase = session.getTransactionSession().getPurchaseLastHistory().getPurchase();
        super.deletePurchaseRequest(session, update, purchase);
    }

    @Override
    public boolean deletePurchaseResult(UserSession session) {
        if (checkCommand(session, PURCHASE_LAST_SPECIAL_CODE)) return false;
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);
        session.getPath().pop();

        transactionCacheService.deletePurchase(session);
        deletionCompleted(session);
        redirectToTable(session);
        return true;
    }

    public boolean scrollNextPurchase(UserSession session) {
        if (!session.getTransactionSession().getPurchaseLastHistory().hasNext()) {
            log.error("Next purchase not found");
            return false;
        }
        transactionCacheService.toNextPurchase(session);
        return workWithPurchaseLast(session);
    }

    public boolean scrollPreviousPurchase(UserSession session) {
        if (!session.getTransactionSession().getPurchaseLastHistory().hasPrevious()) {
            log.error("Previous purchase not found");
            return false;
        }
        transactionCacheService.toPreviousPurchase(session);
        return workWithPurchaseLast(session);
    }

    public void workWithStopButton(UserSession session, String callbackId) {
        answerMessageService.sendSmallWindowAnswer(
                callbackId, STOP_ANSWER_MESSAGE, session.getLocale());
    }


    private static void setUp(UserSession session) {
        String peek = session.getPath().peek();
        if (PURCHASE_LAST_SPECIAL_CODE.equals(peek)
                || PURCHASES_FILTER_BUTTON.equals(peek)) {
            session.getPath().pop();
        }
        session.getPath().push(PURCHASES_INFO_BUTTON);
        session.getTransactionSession().clearPurchaseLastHistory();
    }

    private void sendTable(List<Purchase> purchaseList, UserSession session, Integer messageId) {
        if (purchaseList.isEmpty()) {
            updateMessageService.editInlineMessage(
                    session.getChatId(), messageId,
                    null, getMessage(PURCHASES_TABLE_EMPTY_MESSAGE, session.getLocale()),
                    inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                            List.of(UPDATE_BUTTON, BACK_MODIFY_BUTTON))
            );
        } else {
            String table = createPurchaseTable(session, purchaseList,
                    PURCHASE_LAST_SPECIAL_CODE, true);

            log.info("Showing purchaseLast table for user{}", session.getChatId());
            updateMessageService.editInlineMessage(
                    session.getChatId(), messageId,
                    null, table,
                    inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                            List.of(RULE_BUTTON, UPDATE_BUTTON, BACK_MODIFY_BUTTON))
            );
        }
    }

}
