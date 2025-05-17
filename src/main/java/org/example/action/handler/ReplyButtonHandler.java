package org.example.action.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.action.Action;
import org.example.model.session.UserSession;
import org.example.service.response.FamilySettingsService;
import org.example.service.response.MainResponseService;
import org.example.service.support.special.SpecialTextResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.example.action.tag.CommandKey.FAMILY_COMMAND;
import static org.example.action.tag.ReplyButtonKey.*;

@Slf4j
@Component
public class ReplyButtonHandler {

    @Autowired
    private MainResponseService mainResponseService;

    @Autowired
    private FamilySettingsService familySettingsService;

    @Autowired
    private SpecialTextResponseService specialButtonService;

    private final Map<String, Action> replyButtonActions = new HashMap<>();

    public ReplyButtonHandler() {
        replyButtonActions.put(FAMILY_CREATE_REPLY, this::handleFamilyCreate);
        replyButtonActions.put(FAMILY_JOIN_REPLY, this::handleFamilyJoin);

        replyButtonActions.put(MAIN_QUICK_PURCHASE_REPLY, this::handleQuickPurchaseTab);
        replyButtonActions.put(MAIN_INCOME_REPLY, this::handleIncomeTab);
        replyButtonActions.put(MAIN_PURCHASE_REPLY, this::handlePurchaseTab);
        replyButtonActions.put(MAIN_PROFILE_REPLY, this::handleProfile);
    }

    public void handleReplyButton(UserSession session, Update update) {
        String replyButton = update.getMessage().getText();
        log.info("Received text \"{}\" from user {}", replyButton, session.getChatId());

        Optional.ofNullable(replyButtonActions.get(replyButton))
                .ifPresentOrElse(
                        action -> action.execute(session, update),
                        () -> handleNonTypicalReplyButton(session, update)
                );
    }

    private void handleFamilyCreate(UserSession session, Update update) {
        if (!session.getPath().isEmpty()
                && session.getPath().peek().equals(FAMILY_COMMAND)) {
            familySettingsService.createFamilyCommand(session);
        } else {
            handleNonTypicalReplyButton(session, update);
        }
    }

    private void handleFamilyJoin(UserSession session, Update update) {
        if (!session.getPath().isEmpty()
                && session.getPath().peek().equals(FAMILY_COMMAND)) {
            familySettingsService.joinToFamilyCommand(session);
        } else {
            handleNonTypicalReplyButton(session, update);
        }
    }


    private void handleQuickPurchaseTab(UserSession session, Update update) {
        if (session.getPath().isEmpty()) {
            mainResponseService.quickPurchaseTab(session);
        } else {
            handleNonTypicalReplyButton(session, update);
        }
    }

    private void handleIncomeTab(UserSession session, Update update) {
        if (session.getPath().isEmpty()) {
            mainResponseService.incomeTab(session);
        } else {
            handleNonTypicalReplyButton(session, update);
        }
    }

    private void handlePurchaseTab(UserSession session, Update update) {
        if (session.getPath().isEmpty()) {
            mainResponseService.purchaseTab(session);
        } else {
            handleNonTypicalReplyButton(session, update);
        }
    }

    private void handleProfile(UserSession session, Update update) {
        if (session.getPath().isEmpty()) {
            mainResponseService.profile(session);
        } else {
            handleNonTypicalReplyButton(session, update);
        }
    }


    private void handleNonTypicalReplyButton(UserSession session, Update update) {
        String text = update.getMessage().getText();
        if (!specialButtonService.workWithNonTypicalCommand(session, text)) {
            specialButtonService.answerToIncorrectCommand(session, text);
        }
    }
}
