package org.example.action.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.action.Action;
import org.example.model.session.UserSession;
import org.example.sender.service.UpdateMessageService;
import org.example.service.response.FamilySettingsService;
import org.example.service.response.MainResponseService;
import org.example.service.response.transaction_info.purchase.PurchaseLastService;
import org.example.service.response.transaction_info.purchase.PurchaseSortedService;
import org.example.service.support.special.SpecialTextResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.example.action.tag.CommandKey.*;

@Slf4j
@Component
public class CommandResponseHandler {

    @Autowired
    private SpecialTextResponseService specialTextResponseService;

    @Autowired
    private MainResponseService mainResponseService;

    @Autowired
    private FamilySettingsService familySettingsService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private PurchaseLastService purchaseLastService;

    @Autowired
    private PurchaseSortedService purchaseSortedService;

    private final Map<String, Action> commandActions = new HashMap<>();

    public CommandResponseHandler() {
        commandActions.put(START_COMMAND, this::handleStartCommand);
        commandActions.put(FAMILY_COMMAND, this::handleFamilyCommand);
        commandActions.put(QUICK_COMMAND, this::handleQuickCommand);
        commandActions.put(MAIN_COMMAND, this::handleMainCommand);

        commandActions.put(DELETE_COMMAND, this::handleDeleteCommand);
        commandActions.put(GENERATE_COMMAND, this::handleGenerateCommand);
        commandActions.put(CLEAR_COMMAND, this::handleClearCommand);
    }

    public void handleCommand(UserSession session, Update update) {
        if (session.isBusy()) {
            handleUnavailableCommand(session);
            return;
        }
        String command = update.getMessage().getText();
        log.info("Received command \"{}\" from user {}", command, session.getChatId());

        Optional.ofNullable(commandActions.get(command))
                .ifPresentOrElse(
                        action -> action.execute(session, update),
                        () -> checkIndividualCommand(session, update)
                );
    }

    private void handleStartCommand(UserSession session, Update update) {
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);
        mainResponseService.restartUser(session);
    }

    private void handleFamilyCommand(UserSession session, Update update) {
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);
        familySettingsService.workWithFamilySettingCommand(session);
    }

    private void handleQuickCommand(UserSession session, Update update) {
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);
        mainResponseService.quickPurchaseTab(session);
    }

    private void handleMainCommand(UserSession session, Update update) {
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);
        mainResponseService.toMain(session);
    }

    private void handleDeleteCommand(UserSession session, Update update) {
        if (purchaseLastService.deletePurchaseResult(session)) return;
        if (purchaseSortedService.deletePurchaseResult(session)) return;
        if (familySettingsService.deleteMember(session)) return;
        handleIncorrectCommand(session, update.getMessage().getText());
    }

    private void handleGenerateCommand(UserSession session, Update update) {
        if (specialTextResponseService.generateCommand(session)) return;
        handleIncorrectCommand(session, update.getMessage().getText());
    }

    private void handleClearCommand(UserSession session, Update update) {
        if (specialTextResponseService.clearCommand(session)) return;
        handleIncorrectCommand(session, update.getMessage().getText());
    }


    private void checkIndividualCommand(UserSession session, Update update) {
        String text = update.getMessage().getText();
        if (specialTextResponseService.checkIndividualCommand(session, text)) return;
        handleIncorrectCommand(session, text);
    }


    private void handleIncorrectCommand(UserSession session, String unknownCommand) {
        specialTextResponseService.answerToIncorrectCommand(session, unknownCommand);
    }

    private void handleUnavailableCommand(UserSession session) {
        specialTextResponseService.answerToUnavailableCommand(session);
    }
}
