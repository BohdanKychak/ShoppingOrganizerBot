package org.example.service.response.transaction_info.purchase;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.PictureNotAvailableException;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.model.entity.Purchase;
import org.example.model.entity.UserData;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseLastHistory;
import org.example.repository.PurchaseRepository;
import org.example.sender.service.AnswerMessageService;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.SendPhotoService;
import org.example.sender.service.UpdateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.example.action.tag.CommandKey.DELETE_COMMAND;
import static org.example.action.tag.InlineButtonKey.*;
import static org.example.action.tag.ReplyButtonKey.DESCRIPTION_GIFT_REPLY;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.service.support.PropertiesService.getReplyButton;
import static org.example.util.Constants.*;
import static org.example.util.MessageProperties.*;

@Slf4j
@Component
public abstract class PurchaseInfoService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private AnswerMessageService answerMessageService;

    @Autowired
    private SendPhotoService sendPhotoService;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @SuppressWarnings("unused")
    @Value("${telegram.bot.username}")
    private String botName;

    public abstract void showPurchaseTableInfo(UserSession session, Update update);

    public abstract void showPurchaseTableInfo(UserSession session, Integer messageId);

    public abstract boolean openSelectedPurchaseWithoutMessage(UserSession session);

    public abstract boolean openSelectedPurchase(UserSession session, Long purchaseId);

    public abstract void showReceipt(UserSession session, Update update);

    public abstract void receiptUpdateRequest(UserSession session, Update update, String receivedButton);

    public abstract void receiptUpdateResult(UserSession session, String photo);

    public abstract void descriptionUpdateRequest(UserSession session, Update update);

    public abstract boolean descriptionUpdateResult(UserSession session, String description);

    public abstract void deletePurchaseRequest(UserSession session, Update update);

    public abstract boolean deletePurchaseResult(UserSession session);


    protected boolean workWithPurchaseLast(UserSession session) {
        log.info("Create purchaseLast info for user {}", session.getChatId());
        Purchase purchase = session.getTransactionSession().getPurchaseLastHistory().getPurchase();
        if (isPurchaseExist(purchase)) return false;
        return workWithPurchase(session, purchase,
                inlineCommandHelper.buildInlineKeyboardForPurchaseLast(session.getLocale(),
                        getFirstLineButtons(session),
                        List.of(PURCHASE_DESCRIPTION_BUTTON, PURCHASE_DELETE_BUTTON),
                        List.of(PURCHASES_INFO_BUTTON)));
    }

    protected boolean workWithPurchaseSorted(UserSession session) {
        log.info("Create purchaseSorted info for user {}", session.getChatId());
        Purchase purchase = session.getTransactionSession()
                .getPurchaseSortedHistory().getCurrentPurchase();
        if (isPurchaseExist(purchase)) return false;
        String receiptButton = purchase.getReceiptPhotoId() != null
                ? PURCHASE_RECEIPT_BUTTON : PURCHASE_RECEIPT_ADD_BUTTON;

        return workWithPurchase(session, purchase,
                inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                        List.of(PURCHASE_DESCRIPTION_BUTTON, receiptButton,
                                PURCHASE_DELETE_BUTTON, BACK_MODIFY_BUTTON))
        );
    }

    protected void showReceipt(UserSession session, Purchase purchase, String callbackId) {
        try {
            log.debug("Try to show receipt for user {}", session.getChatId());
            boolean isPurchaseOwner = session.getChatId().equals(purchase.getUserData().getChatId());

            if (DESCRIPTION_GIFT_REPLY.equals(purchase.getDescription()) && !isPurchaseOwner) {
                log.debug("User {} doesn't have access to receipt", session.getChatId());
                throw new Exception("User doesn't have access");
            }
            session.getPath().push(PURCHASE_RECEIPT_BUTTON);
            log.info("Show receipt for user {}", session.getChatId());
            sendPhotoReceipt(session, purchase, isPurchaseOwner);

        } catch (Exception e) {
            answerMessageService.sendSmallWindowAnswer(
                    callbackId, RECEIPT_NOT_AVAILABLE_MESSAGE, session.getLocale());
        }
    }

    protected void receiptUpdateRequest(
            UserSession session, Update update, Purchase purchase, String receivedButton
    ) {
        if (isOwner(session, update.getCallbackQuery().getId(), purchase.getUserData().getChatId())) return;
        log.info("Open receipt update request for user {}", session.getChatId());

        session.getPath().push(receivedButton);
        updateMessageService.editInlineMessage(session.getChatId(), session.getLastMessageId(),
                session.getLocale(), RECEIPT_SEND_PHOTO_MESSAGE, inlineCommandHelper
                        .buildInlineKeyboard(session.getLocale(), List.of(PURCHASE_SELECTED_BUTTON)));
    }

    protected void descriptionUpdateRequest(UserSession session, Update update, Purchase purchase) {
        if (isOwnerDescription(session, update.getCallbackQuery().getId(),
                purchase.getUserData().getChatId(),
                purchase.getDescription(), purchase.getSimpleDescription())
        ) return;
        log.info("Open description update request for user {}", session.getChatId());

        session.getPath().push(PURCHASE_DESCRIPTION_BUTTON);
        updateMessageService.editInlineMessage(session.getChatId(), session.getLastMessageId(),
                null, getDescriptionNewFormat(session.getLocale(), purchase.getDescription()),
                inlineCommandHelper
                        .buildInlineKeyboard(session.getLocale(), List.of(PURCHASE_SELECTED_BUTTON)));
    }

    protected void deletePurchaseRequest(UserSession session, Update update, Purchase purchase) {
        if (isOwner(session, update.getCallbackQuery().getId(), purchase.getUserData().getChatId())) return;
        log.info("Open delete purchase request for user {}", session.getChatId());

        session.getPath().push(PURCHASE_DELETE_BUTTON);
        updateMessageService.editInlineMessage(session.getChatId(), session.getLastMessageId(),
                null, getRequestToDeleteMessage(session.getLocale()),
                inlineCommandHelper
                        .buildInlineKeyboard(session.getLocale(), List.of(PURCHASE_SELECTED_BUTTON)));
    }


    protected String createPurchaseTable(
            UserSession session, List<Purchase> purchaseList,
            String specialCode, boolean needToHide
    ) {
        log.debug("Start creating purchase table for user {}", session.getChatId());
        StringBuilder answer = new StringBuilder(
                createPurchaseTemplate(session.getLocale()));

        for (Purchase purchase : purchaseList) {
            answer.append(NEW_LINE)
                    .append(getLine(session.getFamilyId().toString().hashCode(),
                            purchase, specialCode, needToHide));
        }

        log.debug("Finish creating purchase table for user {}", session.getChatId());
        return answer.toString();
    }

    protected static String getAnswerForNewReceipt(String lastCommand, String photo) {
        if (photo == null) {
            return PURCHASE_DETAILS_NO_ADDED_MESSAGE;
        }
        return switch (lastCommand) {
            case PURCHASE_RECEIPT_ADD_BUTTON -> PURCHASE_RECEIPT_ADDED_MESSAGE;
            case PURCHASE_RECEIPT_REPLACE_BUTTON -> PURCHASE_RECEIPT_UPDATED_MESSAGE;
            default -> PURCHASE_DETAILS_NO_ADDED_MESSAGE;
        };
    }

    protected static String trimDescriptionToMaxLength(String description) {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            return description;
        } else {
            return description.substring(0, MAX_DESCRIPTION_LENGTH).trim();
        }
    }

    protected static String getAnswerForNewDescription(String oldDescription) {
        return oldDescription == null ? PURCHASE_DESCRIPTION_ADDED_MESSAGE : PURCHASE_DESCRIPTION_UPDATED_MESSAGE;
    }

    protected static boolean checkCommand(UserSession session, String code) {
        if (session.getPath().size() < 2) {
            return true;
        }
        String lastCommand = session.getPath().peek();
        String secondLastCommand = session.getPath().peekSecond();
        return !PURCHASE_DELETE_BUTTON.equals(lastCommand)
                || !code.equals(secondLastCommand);
    }

    protected void deletionCompleted(UserSession session) {
        updateMessageService.updateKeyboard(
                session.getChatId(), session.getLastMessageId(), null);
        sendMessageService.sendMessage(session.getChatId(),
                TRANSACTION_DELETED_MESSAGE, session.getLocale());
    }

    protected void redirectToTable(UserSession session) {
        sendMessageService.sendMessage(session.getChatId(),
                getMessage(TEMPORARY_MESSAGE_WAIT_MESSAGE, session.getLocale()));
        showPurchaseTableInfo(session, session.getLastMessageId());
    }


    private static String createPurchaseTemplate(Locale locale) {
        String headerFormat = getMessage(PURCHASES_TABLE_HEADER__FORMAT_MESSAGE, locale);
        String headerData = getMessage(DATE_HEADER, locale);
        String headerNote = getMessage(NOTE_HEADER, locale);
        String headerBill = getMessage(BILL_HEADER, locale);
        String headerAmount = getMessage(AMOUNT_HEADER, locale);

        return String.format(headerFormat, PURCHASE_TABLE_LINK_SYMBOL,
                headerData, headerNote, headerBill, headerAmount);
    }

    private String getLine(
            int familyCode, Purchase purchase, String specialCode, boolean needToHide
    ) {
        return String.format(PURCHASE_TABLE_DATA__FORMAT,
                createPurchaseLink(specialCode,
                        familyCode + SEPARATOR + purchase.getId()),
                purchase.getTimePurchase().format(DATE_TIME_FORMATTER),
                purchase.getDescription() == null ? UNAVAILABLE : AVAILABLE,
                purchase.getReceiptPhotoId() == null ? UNAVAILABLE : AVAILABLE,
                getAmount(purchase, needToHide)
        );
    }

    private String createPurchaseLink(String specialCode, String code) {
        return String.format(BOT_INLINE_LINK__FORMAT,
                String.format(BOT_LINK__FORMAT, botName,
                        SEPARATOR2 + specialCode + SEPARATOR2 + code),
                PURCHASE_TABLE_FILE_SYMBOL
        );
    }

    private static String getAmount(Purchase purchase, boolean needToCheck) {
        if (needToCheck && DESCRIPTION_GIFT_REPLY.equals(purchase.getDescription())) {
            return HIDDEN_PRICE;
        }
        return purchase.getAmount() + SPACE + purchase.getCurrency();
    }

    private static boolean isPurchaseExist(Purchase purchase) {
        if (purchase == null) {
            log.error("Purchase not found");
            return true;
        }
        return false;
    }

    private boolean workWithPurchase(
            UserSession session, Purchase purchase, InlineKeyboardMarkup inlineKeyboard
    ) {
        log.info("Show purchase info for user {}", session.getChatId());
        updateMessageService.editInlineMessage(session.getChatId(),
                session.getLastMessageId(), null,
                getSelectedPurchaseInfo(session, purchase), inlineKeyboard
        );
        return true;
    }

    private String getSelectedPurchaseInfo(UserSession session, Purchase purchase) {
        String dataMissing = getMessage(DATA_MISSING_MESSAGE, session.getLocale());

        return String.format(getMessage(SPECIFIC_PURCHASE_DATA__FORMAT_MESSAGE, session.getLocale()),
                getAmount(purchase, session.getChatId()),
                getDescription(purchase, dataMissing, session.getLocale()),
                purchase.getReceiptPhotoId() == null
                        ? dataMissing
                        : getMessage(RECEIPT_AVAILABLE_MESSAGE, session.getLocale()),
                purchase.getTimePurchase().format(DATE_TIME_FORMATTER_FULL),
                getPurchaseUserName(purchase.getUserData(), session.getLocale()));
    }

    private static String getAmount(Purchase purchase, Long userIdInSelectedPurchase) {
        if (DESCRIPTION_GIFT_REPLY.equals(purchase.getDescription())) {
            if (userIdInSelectedPurchase != null
                    && userIdInSelectedPurchase.equals(purchase.getUserData().getChatId())) {
                return purchase.getAmount() + SPACE + purchase.getCurrency();
            }
            return HIDDEN_PRICE;
        }
        return purchase.getAmount() + SPACE + purchase.getCurrency();
    }

    private static String getDescription(Purchase purchase, String dataMissing, Locale locale) {
        if (purchase.getDescription() != null) {
            if (Boolean.TRUE.equals(purchase.getSimpleDescription())) {
                return getReplyButton(purchase.getDescription(), locale);
            }
            return trimDescriptionToInfoLength(purchase.getDescription());
        }
        return dataMissing;
    }

    private static String trimDescriptionToInfoLength(String description) {
        if (description.length() <= DESCRIPTION_INFO_LENGTH) {
            return description;
        }
        description = description.substring(0, DESCRIPTION_INFO_LENGTH);

        int lastSpaceIndex = description.lastIndexOf(SPACE);
        description = (lastSpaceIndex != -1)
                ? description.substring(0, lastSpaceIndex) : description;

        return description.trim() + ELLIPSIS;
    }

    private String getPurchaseUserName(UserData userData, Locale locale) {
        return userData == null || userData.getUserName() == null
                ? getMessage(DATA_MISSING_MESSAGE, locale)
                : userData.getUserName();
    }

    private static List<String> getFirstLineButtons(UserSession session) {
        List<String> line = new ArrayList<>();
        PurchaseLastHistory purchaseLastHistory = session.getTransactionSession().getPurchaseLastHistory();

        line.add(purchaseLastHistory.hasPrevious() ? PREVIOUS_BUTTON : STOP_BUTTON);
        line.add(purchaseLastHistory.getPurchase().getReceiptPhotoId() != null
                ? PURCHASE_RECEIPT_BUTTON : PURCHASE_RECEIPT_ADD_BUTTON);
        line.add(purchaseLastHistory.hasNext() ? NEXT_BUTTON : STOP_BUTTON);

        return line;
    }

    private void sendPhotoReceipt(UserSession session, Purchase purchase, boolean isPurchaseOwner) {
        updateMessageService.deleteMessage(session.getChatId(), session.getLastMessageId());
        try {
            sendPhotoService.sendPhoto(session.getChatId().toString(),
                    purchase.getTimePurchase().format(DATE_TIME_FORMATTER_FULL), purchase.getReceiptPhotoId());
        } catch (PictureNotAvailableException e) {
            log.error("Error sending, photo unavailable for user {}", session.getChatId());
            sendMessageService.sendMessage(session.getChatId(),
                    PICTURE_NOT_AVAILABLE_MESSAGE, session.getLocale());
            purchase.setReceiptPhotoId(null);
            purchaseRepository.updateReceiptPhotoById(purchase.getId(), null);
        }
        sendNewMessage(session, isPurchaseOwner, purchase.getUserData());
    }

    private void sendNewMessage(UserSession session, boolean isPurchaseOwner, UserData userData) {
        if (isPurchaseOwner) {
            sendMessageService.sendMessage(
                    session.getChatId(), RECEIPT_REPLACE_REQUEST_MESSAGE, session.getLocale(),
                    inlineCommandHelper.buildInlineKeyboard(session.getLocale(),
                            List.of(PURCHASE_RECEIPT_REPLACE_BUTTON, PURCHASE_SELECTED_BUTTON))
            );
        } else {
            sendMessageService.sendMessage(
                    session.getChatId(), PURCHASE_USER__FORMAT_MESSAGE,
                    List.of(getPurchaseUserName(userData, session.getLocale())),
                    session.getLocale(),
                    inlineCommandHelper.buildInlineKeyboard(
                            session.getLocale(), List.of(PURCHASE_SELECTED_BUTTON))
            );
        }
    }

    private boolean isOwner(
            UserSession session, String callbackId, Long userId
    ) {
        if (!session.getChatId().equals(userId)) {
            answerMessageService.sendSmallWindowAnswer(callbackId,
                    ONLY_OWNER_ANSWER_MESSAGE, session.getLocale());
            return true;
        }
        return false;
    }

    private boolean isOwnerDescription(
            UserSession session, String callbackId, Long userId, String description, Boolean isSimple
    ) {
        if (!session.getChatId().equals(userId)) {
            if (description == null
                    || Boolean.TRUE.equals(isSimple)
                    || description.length() < DESCRIPTION_INFO_LENGTH) {
                answerMessageService.sendSmallWindowAnswer(callbackId,
                        ONLY_OWNER_ANSWER_MESSAGE, session.getLocale());
            } else {
                answerMessageService.sendBigWindowAnswer(callbackId, description);
            }
            return true;
        }
        return false;
    }

    private static String getDescriptionNewFormat(Locale locale, String currentDescription) {
        return currentDescription == null
                ? String.format(getMessage(PURCHASE_ADD_DESCRIPTION_WRITE__FORMAT_MESSAGE, locale), MAX_DESCRIPTION_LENGTH)
                : String.format(getMessage(DESCRIPTION_NEW__FORMAT_MESSAGE, locale), MAX_DESCRIPTION_LENGTH, currentDescription);
    }

    private String getRequestToDeleteMessage(Locale locale) {
        return String.format(
                getMessage(TRANSACTION_DELETE_REQUEST__FORMAT_MESSAGE, locale),
                DELETE_COMMAND
        );
    }

}
