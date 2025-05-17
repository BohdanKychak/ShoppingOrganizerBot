package org.example.service.response.transaction_info.purchase;

import lombok.extern.slf4j.Slf4j;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.entity.Purchase;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseSortedHistory;
import org.example.repository.PurchaseRepository;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.example.service.response.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.example.action.tag.InlineButtonKey.PURCHASES_FILTER_BUTTON;
import static org.example.action.tag.InlineButtonKey.PURCHASE_DESCRIPTION_BUTTON;
import static org.example.action.tag.InlineButtonKey.RULE_BUTTON;
import static org.example.enums.Currency.getCurrencyByStringUsed;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.PURCHASE_GENERATE_PATH_CODE;
import static org.example.util.Constants.PURCHASE_SORTED_SPECIAL_CODE;
import static org.example.util.MessageProperties.PURCHASES_TABLE_EMPTY_MESSAGE;
import static org.example.util.MessageProperties.TEMPORARY_MESSAGE_WAIT_MESSAGE;

@Slf4j
@Component
public class PurchaseSortedService extends PurchaseInfoService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Override
    public void showPurchaseTableInfo(UserSession session, Update update) {
        showPurchaseTableInfo(session, update.getCallbackQuery().getMessage().getMessageId());
    }

    @Override
    public void showPurchaseTableInfo(UserSession session, Integer messageId) {
        String peek = session.getPath().peek();
        if (PURCHASE_SORTED_SPECIAL_CODE.equals(peek)) {
            session.getPath().pop();
        }
        session.getPath().push(PURCHASE_GENERATE_PATH_CODE);

        sendTable(session, false);
    }

    @Override
    public boolean openSelectedPurchaseWithoutMessage(UserSession session) {
        if (!PURCHASE_SORTED_SPECIAL_CODE.equals(session.getPath().peek())) {
            return false;
        }
        sendMessageService.sendMessage(session.getChatId(),
                getMessage(TEMPORARY_MESSAGE_WAIT_MESSAGE, session.getLocale()));
        return openSelectedPurchase(session,
                session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchaseId());
    }

    @Override
    public boolean openSelectedPurchase(UserSession session, Long purchaseId) {
        session.getPath().push(PURCHASE_SORTED_SPECIAL_CODE);

        session.getTransactionSession().getPurchaseSortedHistory().setCurrentPurchaseId(purchaseId);

        return workWithPurchaseSorted(session);
    }

    @Override
    public void showReceipt(UserSession session, Update update) {
        Purchase purchase = session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase();
        showReceipt(session, purchase, update.getCallbackQuery().getId());
    }

    @Override
    public void receiptUpdateRequest(UserSession session, Update update, String receivedButton) {
        Purchase purchase = session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase();
        super.receiptUpdateRequest(session, update, purchase, receivedButton);
    }

    @Override
    public void receiptUpdateResult(UserSession session, String photo) {
        String lastCommand = session.getPath().pop();
        saveNewReceipt(session, photo);
        sendMessageService.sendMessage(session.getChatId(),
                getAnswerForNewReceipt(lastCommand, photo), session.getLocale());
        openSelectedPurchaseWithoutMessage(session);
    }

    @Override
    public void descriptionUpdateRequest(UserSession session, Update update) {
        Purchase purchase = session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase();
        super.descriptionUpdateRequest(session, update, purchase);
    }

    @Override
    public boolean descriptionUpdateResult(UserSession session, String description) {
        log.debug("Check message {} in descriptionUpdate purchaseSorted tab", description);
        if (!PURCHASE_DESCRIPTION_BUTTON.equals(session.getPath().peek())
                || !session.getPath().contains(PURCHASE_SORTED_SPECIAL_CODE)) {
            return false;
        }
        session.getPath().pop();
        String oldDescription = session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase().getDescription();
        saveNewDescription(session, trimDescriptionToMaxLength(description));

        updateMessageService.deleteMessage(session.getChatId(), session.getLastMessageId());
        sendMessageService.sendMessage(session.getChatId(),
                getAnswerForNewDescription(oldDescription), session.getLocale());
        openSelectedPurchaseWithoutMessage(session);
        return true;
    }

    @Override
    public void deletePurchaseRequest(UserSession session, Update update) {
        Purchase purchase = session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase();
        super.deletePurchaseRequest(session, update, purchase);
    }

    @Override
    public boolean deletePurchaseResult(UserSession session) {
        if (checkCommand(session, PURCHASE_SORTED_SPECIAL_CODE)) return false;
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);
        session.getPath().pop();

        deletePurchase(session);
        deletionCompleted(session);
        redirectToTable(session);
        return true;
    }

    public void sendTable(UserSession session, boolean isNew) {
        PurchaseSortedHistory history = session.getTransactionSession().getPurchaseSortedHistory();
        history.setCurrentPurchaseId(null);
        List<Purchase> purchaseList = history.getSortedPurchasesList();

        String text;
        if (purchaseList.isEmpty()) {
            text = getMessage(PURCHASES_TABLE_EMPTY_MESSAGE, session.getLocale());
        } else {
            log.info("Showing purchaseSorted table for user{}", session.getChatId());
            text = createPurchaseTable(session, purchaseList,
                    PURCHASE_SORTED_SPECIAL_CODE, false);
        }

        if (isNew) {
            log.debug("Sending new purchaseSorted table for user {}", session.getChatId());
            sendMessageService.sendMessage(session.getChatId(), text, null,
                    inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                            List.of(RULE_BUTTON, PURCHASES_FILTER_BUTTON))
            );
        } else {
            log.debug("Updating purchaseSorted table for user {}", session.getChatId());
            updateMessageService.editInlineMessage(session.getChatId(),
                    session.getLastMessageId(), null, text,
                    inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                            List.of(RULE_BUTTON, PURCHASES_FILTER_BUTTON)));
        }
    }


    private void saveNewReceipt(UserSession session, String photo) {
        Purchase purchase = session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase();
        purchase.setReceiptPhotoId(photo);
        purchaseRepository.updateReceiptPhotoById(purchase.getId(), photo);
    }

    private void saveNewDescription(UserSession session, String description) {
        Purchase purchase = session.getTransactionSession().getPurchaseSortedHistory().getCurrentPurchase();
        purchase.setDescription(description);
        purchaseRepository.updateDescriptionById(purchase.getId(), description, false);
    }

    private void deletePurchase(UserSession session) {
        PurchaseSortedHistory purchaseSortedHistory = session.getTransactionSession().getPurchaseSortedHistory();
        Purchase purchase = purchaseSortedHistory.getCurrentPurchase();

        purchaseRepository.delete(purchase);
        accountService.addMoneyToAccount(purchase.getAmount().doubleValue(),
                getCurrencyByStringUsed(purchase.getCurrency()), session.getFamilyId());

        purchaseSortedHistory.getSortedPurchasesMap().remove(purchase.getId());
        purchaseSortedHistory.getSortedPurchasesList().remove(purchase);
        purchaseSortedHistory.setCurrentPurchaseId(null);
    }
}
