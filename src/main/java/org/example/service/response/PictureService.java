package org.example.service.response;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.PictureNotAvailableException;
import org.example.model.session.UserSession;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.SendPhotoService;
import org.example.service.response.transaction_info.purchase.PurchaseLastService;
import org.example.service.response.transaction_info.purchase.PurchaseSortedService;
import org.example.service.support.special.UnknownMessageService;
import org.example.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import static org.example.action.tag.InlineButtonKey.PURCHASE_RECEIPT_ADD_BUTTON;
import static org.example.action.tag.InlineButtonKey.PURCHASE_RECEIPT_REPLACE_BUTTON;
import static org.example.util.Constants.PURCHASE_LAST_SPECIAL_CODE;
import static org.example.util.Constants.PURCHASE_SORTED_SPECIAL_CODE;
import static org.example.util.MessageProperties.SOMETHING_WENT_WRONG_MESSAGE;

@Slf4j
@Component
public class PictureService {

    @Autowired
    private SendPhotoService sendPhotoService;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private MoneyTransactionService moneyTransactionService;

    @Autowired
    private PurchaseLastService purchaseLastService;

    @Autowired
    private PurchaseSortedService purchaseSortedService;

    @Autowired
    private UnknownMessageService unknownMessageService;

    @SuppressWarnings("unused")
    @Value("${telegram.data.channel.id}")
    private String channelId;

    public void workWithPictures(UserSession session, Message message) {
        log.info("Started working with pictures for user: {}", session.getChatId());
        if (session.getTransactionSession().getPurchaseCreation() != null && session.isBusy()
                && session.getTransactionSession().getPurchaseCreation().getHasDescription() != null
                && session.getTransactionSession().getPurchaseCreation().getReceiptPhotoId() == null) {
            log.info("The image has been redirected to moneyTransactionService for user {}", session.getChatId());
            moneyTransactionService.setPurchaseReceiptPhoto(session,
                    savePurchaseReceiptPhoto(session, message));
            return;
        }
        if (PURCHASE_RECEIPT_REPLACE_BUTTON.equals(session.getPath().peek())
                || PURCHASE_RECEIPT_ADD_BUTTON.equals(session.getPath().peek())) {
            if (workWithReceipt(session, message)) {
                log.debug("Finished working with the image from user {} in purchaseInfo", session.getChatId());
                return;
            }
        }
        log.info("Photo not expected for user: {}", session.getChatId());
        photoNotExpected(session, message);
    }

    private boolean workWithReceipt(UserSession session, Message message) {
        if (session.getPath().size() >= 2) {
            String secondPathElement = session.getPath().peekSecond();
            if (PURCHASE_LAST_SPECIAL_CODE.equals(secondPathElement)) {
                log.info("The image has been redirected to purchaseLastInfo for user {}", session.getChatId());
                purchaseLastService.receiptUpdateResult(session,
                        savePurchaseReceiptPhoto(session, message));
                return true;
            }
            if (PURCHASE_SORTED_SPECIAL_CODE.equals(secondPathElement)) {
                log.info("The image has been redirected to purchaseSortedInfo for user {}", session.getChatId());
                purchaseSortedService.receiptUpdateResult(session,
                        savePurchaseReceiptPhoto(session, message));
                return true;
            }
        }
        return false;
    }

    private String savePurchaseReceiptPhoto(UserSession session, Message message) {
        String photo = message.getPhoto().get(0).getFileId();
        log.debug("Saving purchase receipt photo {} for user: {}",
                photo, session.getChatId());
        try {
            sendPhotoService.sendPhoto(channelId,
                    session.getChatId() + Constants.SEPARATOR + photo, photo);
        } catch (PictureNotAvailableException e) {
            log.error("Error sending photo {} to channel ", photo);
            sendMessageService.sendMessage(session.getChatId(),
                    SOMETHING_WENT_WRONG_MESSAGE, session.getLocale());
            return null;
        }
        return photo;
    }

    private void photoNotExpected(UserSession session, Message message) {
        unknownMessageService.unknownMessage(session, message);
    }

}
