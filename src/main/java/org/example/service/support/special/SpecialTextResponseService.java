package org.example.service.support.special;

import lombok.extern.slf4j.Slf4j;
import org.example.model.session.UserSession;
import org.example.sender.service.SendMessageService;
import org.example.service.response.CurrencyService;
import org.example.service.response.FamilySettingsService;
import org.example.service.response.MoneyTransactionService;
import org.example.service.response.transaction_info.purchase.PurchaseFilterService;
import org.example.service.response.transaction_info.purchase.PurchaseInfoService;
import org.example.service.response.transaction_info.purchase.PurchaseLastService;
import org.example.service.response.transaction_info.purchase.PurchaseSortedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.example.action.tag.InlineButtonKey.FAMILY_MEMBERS_INFO_BUTTON;
import static org.example.action.tag.InlineButtonKey.PURCHASES_FILTER_BUTTON;
import static org.example.action.tag.InlineButtonKey.PURCHASES_INFO_BUTTON;
import static org.example.util.Constants.*;
import static org.example.util.MessageProperties.INCORRECT_COMMAND_MESSAGE;
import static org.example.util.MessageProperties.UNAVAILABLE_ACTION_MESSAGE;

@Slf4j
@Component
public class SpecialTextResponseService {

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private FamilySettingsService familySettingsService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private MoneyTransactionService moneyTransactionService;

    @Autowired
    private PurchaseLastService purchaseLastService;

    @Autowired
    private PurchaseFilterService purchaseFilterService;

    @Autowired
    private PurchaseSortedService purchaseSortedService;

    public boolean workWithNonTypicalCommand(UserSession userSession, String receivedText) {
        return familySettingsService.nonTypicalFamilyCommand(userSession, receivedText)
                || moneyTransactionService.moneyTransactionMessage(userSession, receivedText)
                || purchaseLastService.descriptionUpdateResult(userSession, receivedText)
                || purchaseSortedService.descriptionUpdateResult(userSession, receivedText)
                || purchaseFilterService.checkNonTypicalPurchaseFilterCommand(userSession, receivedText)
                || currencyService.currencyCalculator(userSession, receivedText);
    }

    public void answerToIncorrectCommand(UserSession session, String text) {
        log.info("Unknown or unavailable command: \"{}\" from {}", text, session.getChatId());
        sendMessageService.sendMessageNotSaveMessage(session.getChatId(),
                INCORRECT_COMMAND_MESSAGE, session.getLocale());
    }

    public void answerToUnavailableCommand(UserSession session) {
        log.info("Attempt to execute an unavailable command from {}", session.getChatId());
        sendMessageService.sendMessageNotSaveMessage(session.getChatId(),
                UNAVAILABLE_ACTION_MESSAGE, session.getLocale());
    }

    public boolean checkIndividualCommand(UserSession session, String text) {
        log.debug("Check message {} in IndividualCommand", text);
        String command = session.getPath().peek();
        if (PURCHASES_INFO_BUTTON.equals(command)) {
            if (selectedPurchaseInfo(session, text,
                    PURCHASE_LAST_SPECIAL_CODE, purchaseLastService)) return true;
        }
        if (PURCHASE_GENERATE_PATH_CODE.equals(command)) {
            if (selectedPurchaseInfo(session, text,
                    PURCHASE_SORTED_SPECIAL_CODE, purchaseSortedService)) return true;
        }
        if (FAMILY_MEMBERS_INFO_BUTTON.equals(command)) {
            return deleteMemberRequest(session, text);
        }
        return false;
    }

    public boolean generateCommand(UserSession session) {
        if (PURCHASES_FILTER_BUTTON.equals(session.getPath().peek())) {
            purchaseFilterService.generateTable(session);
            return true;
        }
        return false;
    }

    public boolean clearCommand(UserSession session) {
        if (PURCHASES_FILTER_BUTTON.equals(session.getPath().peek())) {
            session.getTransactionSession().clearPurchaseSortedHistory();
            log.info("Clear purchase filter for {}", session.getChatId());
            purchaseFilterService.showFilterMenu(session);
            return true;
        }
        return false;
    }

    private boolean selectedPurchaseInfo(
            UserSession session, String text,
            String specialCode, PurchaseInfoService purchaseInfoService
    ) {
        log.info("Try to open selected purchase info for {}", session.getChatId());
        String[] command = text.split(SEPARATOR2);
        if (command.length == 3 && specialCode.equals(command[1])) {
            String[] code = command[2].split(SEPARATOR);
            if (getFamilyHashCode(session).equals(code[0])) {
                log.info("Open selected purchase info for {}", session.getChatId());
                return purchaseInfoService
                        .openSelectedPurchase(session, Long.valueOf(code[1]));
            }
        }
        log.info("Purchase not found for {}", session.getChatId());
        return false;
    }

    private static String getFamilyHashCode(UserSession session) {
        return String.valueOf(session.getFamilyId().toString().hashCode());
    }

    private boolean deleteMemberRequest(UserSession session, String text) {
        String[] command = text.split(SEPARATOR2);
        if (command.length == 3 && DELETE_MEMBER_SPECIAL_CODE.equals(command[1])) {
            return familySettingsService.deleteMemberRequest(session, command[2]);
        }
        return false;
    }

}
