package org.example.service.response;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.helper.keyboard.InlineCommandHelper;
import org.example.helper.keyboard.ReplyKeyboardHelper;
import org.example.model.helper.SessionPathDeque;
import org.example.model.entity.FamilyData;
import org.example.model.entity.UserData;
import org.example.model.session.UserSession;
import org.example.model.session.purchase.PurchaseCreation;
import org.example.repository.FamilyDataRepository;
import org.example.repository.UserDataRepository;
import org.example.sender.service.SendMessageService;
import org.example.sender.service.UpdateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.example.action.tag.CommandKey.FAMILY_COMMAND;
import static org.example.action.tag.InlineButtonKey.*;
import static org.example.action.tag.ReplyButtonKey.FAMILY_CREATE_REPLY;
import static org.example.action.tag.ReplyButtonKey.FAMILY_JOIN_REPLY;
import static org.example.action.tag.ReplyButtonKey.MAIN_PROFILE_REPLY;
import static org.example.bot.Dispatcher.USER_SESSION_MAP;
import static org.example.service.support.EncryptionService.decrypt;
import static org.example.service.support.EncryptionService.encrypt;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.*;
import static org.example.util.MessageProperties.*;

@Slf4j
@Component
public class FamilySettingsService {

    private static final Set<Long> FAMILY_IDS_SET = new HashSet<>();

    @Autowired
    private FamilyDataRepository familyDataRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private SendMessageService sendMessageService;

    @Autowired
    private UpdateMessageService updateMessageService;

    @Autowired
    private ReplyKeyboardHelper replyKeyboardHelper;

    @Autowired
    private InlineCommandHelper inlineCommandHelper;

    @Autowired
    private MainResponseService mainResponseService;

    @Autowired
    private AccountService accountService;

    @SuppressWarnings("unused")
    @Value("${telegram.bot.username}")
    private String botName;

    private static Long familyCode;

    @SuppressWarnings("unused")
    @PostConstruct
    public void init() {
        FAMILY_IDS_SET.addAll(familyDataRepository.findAllFamilyIds());
        familyCode = FAMILY_IDS_SET.isEmpty()
                ? FIRST_FAMILY_ID : Collections.max(FAMILY_IDS_SET);
    }

    public void workWithFamilySettingCommand(UserSession session) {
        log.info("Opening family tab by command for user {}", session.getChatId());
        getFamilyInfoCommand(session, MAIN_PROFILE_REPLY.equals(session.getPath().peek()));
    }

    public void workWithFamilySettingButton(UserSession userSession, Integer messageId) {
        log.info("Opening family tab by button for user {}", userSession.getChatId());
        userSession.getPath().push(FAMILY_SETTINGS_BUTTON);
        updateMessageService.deleteMessage(userSession.getChatId(), messageId);
        sendMessageService.sendMessage(
                userSession.getChatId(), FAMILY_SETTINGS_OPEN_MESSAGE,
                userSession.getLocale(), true
        );
        getFamilyInfoCommand(userSession, true);
    }

    public boolean nonTypicalFamilyCommand(UserSession session, String receivedText) {
        log.debug("Check message {} in family tab", receivedText);
        if (setNewFamilyName(session, receivedText)) return true;
        if (checkFamilyCreateCaptcha(session, receivedText)) return true;
        return enterFamilyCode(session, receivedText);
    }

    public void openChangeFamilyName(UserSession userSession, Integer messageId) {
        log.info("Open family name change tab for user {}", userSession.getChatId());
        userSession.getPath().push(FAMILY_CHANGE_NAME_BUTTON);
        updateMessageService.editInlineMessage(
                userSession.getChatId(), messageId,
                userSession.getLocale(), FAMILY_CHANGE_NAME_CREATE_MESSAGE,
                inlineCommandHelper.buildInlineKeyboard(userSession.getLocale(), BACK_REMOVE_BUTTON)
        );
    }

    public void familyMembersList(UserSession userSession, Integer messageId) {
        log.info("Open family members tab for user {}", userSession.getChatId());
        updateMessageService.updateKeyboard(
                userSession.getChatId(), messageId, null);
        userSession.getPath().push(FAMILY_MEMBERS_INFO_BUTTON);
        String members = getFamilyMembersList(userSession);
        sendMessageService.sendMessage(userSession.getChatId(),
                FAMILY_MEMBER_LIST__FORMAT_MESSAGE,
                List.of(members),
                userSession.getLocale());
        sendMessageService.sendMessage(userSession.getChatId(),
                FAMILY_MEMBER_SHARE_MESSAGE, userSession.getLocale(),
                inlineCommandHelper.buildInlineKeyboard(userSession.getLocale(),
                        List.of(BOT_INFO_BUTTON, BACK_REMOVE_BUTTON))
        );
    }

    public void leaveFamily(
            UserSession userSession, Integer messageId, boolean selfDeletion
    ) {
        log.info("The process of removing the user {} from the family {} has begun",
                userSession.getChatId(), userSession.getFamilyId());
        if (userSession.getFamilyId() != null) {
            FamilyData familyData = familyDataRepository
                    .findById(userSession.getFamilyId()).orElse(null);
            UserData userData = userDataRepository
                    .findById(userSession.getChatId()).orElse(null);

            if (familyData == null || userData == null) {
                log.error("Family or user not found for user {}", userSession.getChatId());
                return;
            }
            familyData.getChatIds().remove(userData.getChatId());
            if (!selfDeletion) {
                log.info("Create new passcode for family {}", familyData.getFamilyId());
                familyData.setPassCode(createRandomPass());
            }
            log.debug("Save family data {} for user {}", familyData, userSession.getChatId());
            familyDataRepository.save(familyData);
            userDataRepository.updateFamilyIdByChatId(userSession.getChatId(), null);
        }
        log.info("User {} has been removed from the family", userSession.getChatId());
        userSession.setFamilyId(null);
        userSession.userIsBusy();
        updateMessageService.editInlineMessage(
                userSession.getChatId(), messageId,
                userSession.getLocale(), FAMILY_LEAVE_MESSAGE
        );
        setUpFamily(userSession.getChatId(), userSession.getLocale());
    }

    public void createFamilyCommand(UserSession session) {
        if (session.getFamilyId() != null) {
            alreadyIn(session);
            return;
        }
        log.info("Open family creation tab for user {}", session.getChatId());
        List<FamilyData> families = familyDataRepository
                .findAllFamiliesByCreatorId(session.getChatId());
        if (families.size() >= FAMILIES_MAX_LIMIT) {
            enoughFamilies(session, families);
            return;
        }
        goToCaptcha(session);
    }

    public void joinToFamilyCommand(UserSession session) {
        if (session.getFamilyId() != null) {
            alreadyIn(session);
            return;
        }
        log.info("Open family join tab for user {}", session.getChatId());
        sendMessageService.sendTemporaryMessage(session.getChatId(),
                TEMPORARY_MESSAGE_WAIT_MESSAGE, session.getLocale());
        session.getPath().push(FAMILY_JOIN_REPLY);
        sendMessageService.sendMessage(session.getChatId(),
                FAMILY_JOIN_MESSAGE, session.getLocale(),
                replyKeyboardHelper.buildKeyboardOnePerLine(
                        session.getLocale(), List.of(FAMILY_CREATE_REPLY))
        );
    }

    public boolean deleteMemberRequest(UserSession session, String code) {
        if (!FAMILY_MEMBERS_INFO_BUTTON.equals(session.getPath().peek())) {
            return false;
        }
        log.info("Delete family member request for user {}", session.getChatId());
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);

        Long memberId = getMemberId(session, code);
        UserData userData = memberId == null ? null
                : userDataRepository.findById(memberId).orElse(null);

        if (userData == null) {
            log.warn("User not found for user {}", session.getChatId());
            sendMessageService.sendMessage(session.getChatId(),
                    SOMETHING_WENT_WRONG_MESSAGE, session.getLocale());
            sendMessageService.sendMessage(session.getChatId(),
                    TEMPORARY_MESSAGE_WAIT_MESSAGE, session.getLocale());
            familyMembersList(session, session.getLastMessageId());
            return true;
        }
        session.getPath().push(DELETE_MEMBER_SPECIAL_CODE);
        session.setTemporaryData(userData.getChatId().toString());
        sendMessageService.sendMessage(session.getChatId(),
                FAMILY_MEMBER_REMOVE_REQUEST__FORMAT_MESSAGE, List.of(userData.getUserName()),
                session.getLocale(), inlineCommandHelper
                        .buildInlineKeyboard(session.getLocale(), BACK_REMOVE_BUTTON));
        return true;
    }

    public boolean deleteMember(UserSession session) {
        Long memberId;
        try {
            if (DELETE_MEMBER_SPECIAL_CODE.equals(session.getPath().peek())) {
                session.getPath().pop();
            } else {
                return false;
            }
            if (session.getTemporaryData() == null) return false;
            memberId = Long.parseLong(session.getTemporaryData());
        } catch (Exception e) {
            return false;
        }
        updateMessageService.updateKeyboard(session.getChatId(), session.getLastMessageId(), null);

        UserSession memberSession = USER_SESSION_MAP.get(memberId);
        if (memberSession == null) {
            log.info("User {} is not in the session map", memberId);
            UserData userData = userDataRepository.findById(memberId).orElse(null);
            if (userData != null) {
                memberSession = UserSession.createUserSessionByUserData(userData);
            }
        }

        if (memberSession != null &&
                session.getFamilyId().equals(memberSession.getFamilyId())) {
            removeMember(session.getFamilyId(), memberId, memberSession);
            sendMessageService.sendMessage(session.getChatId(),
                    FAMILY_PASSCODE_UPDATED_MESSAGE, session.getLocale());
        }

        sendMessageService.sendMessage(session.getChatId(),
                FAMILY_MEMBER_UPDATED_MESSAGE, session.getLocale());
        familyMembersList(session, session.getLastMessageId());
        return true;
    }

    private void removeMember(Long familyId, Long memberId, UserSession memberSession) {
        log.info("Start removing family member {} from family {}", memberId, familyId);
        sendMessageService.sendMessage(memberId,
                FAMILY_REMOVED_MESSAGE, memberSession.getLocale());
        boolean memberIsBusy = memberSession.isBusy();
        PurchaseCreation purchase = memberSession
                .getTransactionSession().getPurchaseCreation();
        sendMessageService.sendMessage(memberId,
                WAIT_PROCESS_FINISH_MESSAGE, memberSession.getLocale());
        leaveFamily(memberSession, memberSession.getLastMessageId(), false);
        log.info("Family member {} has been removed from family {}", memberId, familyId);

        if (memberIsBusy && purchase != null) {
            log.info("Forcefully end the transaction of a deleted family member {}", memberId);
            accountService.addMoneyToAccount(purchase.getAmount(),
                    purchase.getCurrency(), familyId);
            memberSession.getTransactionSession().clearPurchaseCreation();
        }
    }

    private static Long getMemberId(UserSession session, String code) {
        Long memberId = tryDecryptAndParse(code, session.getChatId().toString());
        return (memberId != null) ? memberId : tryParseLong(code);
    }

    private static Long tryDecryptAndParse(String encryptedCode, String key) {
        try {
            return Long.parseLong(decrypt(encryptedCode, key));
        } catch (Exception e) {
            log.error("Error decrypting code: {}", e.getMessage());
            return null;
        }
    }

    private static Long tryParseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            log.error("Error parsing value to Long: {}", e.getMessage());
            return null;
        }
    }

    private void alreadyIn(UserSession session) {
        log.info("User {} tried to connect to family, but user has been already in family", session.getChatId());
        sendMessageService.sendMessageNotSaveMessage(
                session.getChatId(), FAMILY_ALREADY_IN_MESSAGE, session.getLocale());
    }

    private void enoughFamilies(UserSession session, List<FamilyData> familyDataList) {
        log.info("User {} has already created maximum number of families", session.getChatId());
        sendMessageService.sendMessage(
                session.getChatId(), FAMILY_SUFFICIENT_NUMBER__FORMAT_MESSAGE,
                List.of(getFamilyCodes(familyDataList)), session.getLocale(),
                replyKeyboardHelper.buildKeyboardOnePerLine(session.getLocale(),
                        List.of(FAMILY_JOIN_REPLY))
        );
    }

    private void goToCaptcha(UserSession session) {
        log.info("Open family captcha for user {}", session.getChatId());
        session.getPath().push(FAMILY_CREATE_CAPTCHA);
        sendMessageService.sendMessage(session.getChatId(),
                FAMILY_PASS_CAPTCHA_MESSAGE, session.getLocale());
        captchaMessage(session);
    }

    private void captchaMessage(UserSession session) {
        sendMessageService.sendMessage(
                session.getChatId(), generateCaptchaText(session),
                replyKeyboardHelper.buildKeyboardOnePerLine(session.getLocale(),
                        List.of(FAMILY_JOIN_REPLY))
        );
    }

    private static String generateCaptchaText(UserSession session) {
        Random random = new Random();
        int num1 = random.nextInt(9) + 1; // first number (from 1 to 9)
        int num2 = random.nextInt(9) + 1; // second number (from 1 to 9)
        boolean isAddition = random.nextBoolean();

        String operator = isAddition ? PLUS : MINUS;

        int result = isAddition ? num1 + num2 : num1 - num2;
        session.setTemporaryData(String.valueOf(result));

        log.debug("Generated captcha: {} {} {} = {}", num1, operator, num2, result);
        return num1 + SPACE + operator + SPACE + num2;
    }

    private boolean checkFamilyCreateCaptcha(UserSession session, String receivedText) {
        if (!FAMILY_CREATE_CAPTCHA.equals(session.getPath().peek())) {
            return false;
        }
        if (FAMILY_JOIN_REPLY.equals(receivedText)) {
            log.info("Open join family menu for user {}", session.getChatId());
            session.getPath().pop();
            joinToFamilyCommand(session);
            return true;
        }
        log.info("Check family captcha for user {}", session.getChatId());
        try {
            int answer = Integer.parseInt(session.getTemporaryData());
            int userAnswer = Integer.parseInt(receivedText);
            if (answer == userAnswer) {
                log.info("Family captcha is correct for user {}", session.getChatId());
                workWithCorrectFamilyCaptcha(session);
                return true;
            }
        } catch (Exception e) {
            log.warn("incorrect answer format{}", receivedText);
        }
        log.info("Family captcha is incorrect for user {}", session.getChatId());
        workWithIncorrectFamilyCaptcha(session);
        return true;
    }

    private void workWithCorrectFamilyCaptcha(UserSession session) {
        session.setTemporaryData(null);
        createFamily(session);
    }

    private void workWithIncorrectFamilyCaptcha(UserSession session) {
        sendMessageService.sendMessage(session.getChatId(),
                FAMILY_INCORRECT_CAPTCHA_MESSAGE, session.getLocale());
        captchaMessage(session);
    }

    private void createFamily(UserSession session) {
        log.info("Start family creation for user {}", session.getChatId());
        Long familyId = getFamilyId();
        String familyPass = createRandomPass();
        createNewFamilyInDataBase(familyId, familyPass, session.getChatId());
        saveFamilyInUserData(session, familyId);
        FAMILY_IDS_SET.add(familyId);
        session.setFamilyId(familyId);
        session.getPath().clear();
        session.userIsFree();
        mainResponseService.sendMessageWithMainCommands(
                session.getChatId(),
                FAMILY_RECEIVED__FORMAT_MESSAGE,
                List.of(familyId + SEPARATOR + familyPass),
                session.getLocale()
        );
        log.info("Family {} has been created for user {}", familyId, session.getChatId());
    }

    private String getFamilyCodes(List<FamilyData> familyDataList) {
        StringBuilder familyCodesString = new StringBuilder();
        for (FamilyData family : familyDataList) {
            familyCodesString.append(String.format(FAMILY_CODES__FORMAT,
                    family.getFamilyId() + SEPARATOR + family.getPassCode()));
        }
        return familyCodesString.toString();
    }

    private boolean setNewFamilyName(UserSession userSession, String receivedText) {
        if (FAMILY_CHANGE_NAME_BUTTON.equals(userSession.getPath().peek())) {
            log.info("Start to work with family name change for user {}", userSession.getChatId());
            if (receivedText.length() > FAMILY_MAX_LENGTH_NAME
                    || receivedText.length() < FAMILY_MIN_LENGTH_NAME) {
                log.warn("Family name length is wrong for user {}", userSession.getChatId());
                sendMessageService.sendMessage(userSession.getChatId(),
                        FAMILY_NAME_LENGTH_WRONG_MESSAGE, userSession.getLocale());
                return true;
            }

            FamilyData familyData = familyDataRepository
                    .findById(userSession.getFamilyId()).orElse(null);
            if (familyData == null) {
                log.error("Family not found for user {}", userSession.getChatId());
                return false;
            }
            log.debug("Try to change family name to {} for user {}",
                    receivedText, userSession.getChatId());
            familyData.setFamilyName(receivedText);
            familyDataRepository.save(familyData);
            userSession.getPath().pop();

            log.info("Family name changed for user {}", userSession.getChatId());
            sendMessageService.sendMessage(userSession.getChatId(),
                    FAMILY_CHANGE_NAME_SUCCESS_MESSAGE, userSession.getLocale());
            sendFamilyInfo(userSession, familyData, true);
            return true;
        }
        return false;
    }

    private boolean enterFamilyCode(UserSession session, String receivedText) {
        if (session.getFamilyId() != null
                || !FAMILY_JOIN_REPLY.equals(session.getPath().peek())) {
            return false;
        }
        if (FAMILY_CREATE_REPLY.equals(receivedText)) {
            session.getPath().pop();
            createFamilyCommand(session);
            return true;
        }

        Long familyId = getValidFamilyId(receivedText, session.getChatId());
        if (familyId == null) {
            sendMessageService.sendMessageNotSaveMessage(session.getChatId(),
                    FAMILY_JOIN_WRONG_CODE_MESSAGE, session.getLocale());
            return true;
        }

        updateMessageService.updateKeyboard(session.getChatId(),
                session.getLastMessageId(), null);
        joinToFamily(session, familyId);

        log.info("{} connect to family{}", session.getName(), familyId);
        mainResponseService.sendMessageWithMainCommands(
                session.getChatId(),
                FAMILY_RECEIVED__FORMAT_MESSAGE,
                List.of(receivedText),
                session.getLocale()
        );
        return true;
    }

    private void joinToFamily(UserSession session, Long familyId) {
        UserData userData = userDataRepository
                .findById(session.getChatId())
                .orElseGet(() -> new UserData(session.getChatId(),
                        session.getName(), session.getLocale().toString()));
        userData.setFamilyId(familyId);
        userDataRepository.save(userData);

        session.setFamilyId(familyId);
        session.getPath().clear();
        session.userIsFree();
    }

    private String getFamilyMembersList(UserSession session) {
        FamilyData familyData = familyDataRepository.findFamilyById(session.getFamilyId());
        boolean creator = session.getChatId().equals(familyData.getCreatorId());
        log.debug("Family {} and user {} are{} creator",
                familyData.getFamilyId(), session.getChatId(), creator ? "" : " not");

        List<UserData> membersList = userDataRepository
                .findAllUsersByFamilyId(session.getFamilyId());
        StringBuilder membersString = new StringBuilder();
        for (int i = 0; i < membersList.size(); i++) {
            UserData member = membersList.get(i);
            log.debug("Add family member {} to list", member.getUserName());
            membersString.append(String.format(FAMILY_MEMBER_TEMPLATE,
                    i + 1, member.getUserName()));
            if (creator && !session.getChatId().equals(member.getChatId())) {
                membersString.append(SPACE)
                        .append(createMemberDeleteLink(getEncrypt(session, member)));
            }
            membersString.append(NEW_LINE);
        }
        if (membersList.size() > 1 && creator) {
            log.info("Add family member list creator message");
            membersString.append(NEW_LINE).append(
                    getMessage(FAMILY_MEMBER_LIST_CREATOR_MESSAGE, session.getLocale()));
        }
        return membersString.toString();
    }

    // only one thread can get code at the same time
    private synchronized static Long getFamilyId() {
        return ++familyCode;
    }

    private synchronized static String createRandomPass() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(FAMILY_PASS_LENGTH);

        for (int i = 0; i < FAMILY_PASS_LENGTH; i++) {
            int randomIndex = random.nextInt(SYMBOLS_FOR_PASS_GENERATE.length());
            password.append(SYMBOLS_FOR_PASS_GENERATE.charAt(randomIndex));
        }
        log.debug("Created new family passcode {}", password);
        return password.toString();
    }

    private void createNewFamilyInDataBase(Long familyId, String password, Long chatId) {
        familyDataRepository.save(
                new FamilyData(familyId, password,
                        FAMILY_START_NAME_TEMPLATE + familyId,
                        Set.of(chatId), Set.of(), chatId));
    }

    private void saveFamilyInUserData(UserSession userSession, Long familyId) {
        UserData userData = userDataRepository
                .findById(userSession.getChatId())
                .orElseGet(() ->
                        new UserData(userSession.getChatId(),
                                userSession.getName(), userSession.getLocale().toString()));
        userData.setFamilyId(familyId);
        userDataRepository.save(userData);
    }

    private void getFamilyInfoCommand(UserSession session, boolean withSettingsKeyboard) {
        if (session.getPath().isEmpty()) {
            session.getPath().push(FAMILY_COMMAND);
        }
        if (session.getFamilyId() == null) {
            session.userIsBusy();
            askFamilyId(session.getChatId(), session.getLocale());
        } else {
            familyInfo(session, withSettingsKeyboard);
        }
    }

    private void setUpFamily(Long chatId, Locale locale) {
        if (USER_SESSION_MAP.get(chatId) != null) {
            USER_SESSION_MAP.get(chatId).getPath().clear();
            USER_SESSION_MAP.get(chatId).getPath().push(FAMILY_COMMAND);
            askFamilyId(chatId, locale);
        }
    }

    private void askFamilyId(Long chatId, Locale locale) {
        log.info("Asking setup family for user {}", chatId);
        sendMessageService.sendMessage(
                chatId, FAMILY_START_SETTINGS_MESSAGE, locale,
                replyKeyboardHelper.buildKeyboardOnePerLine(locale,
                        List.of(FAMILY_CREATE_REPLY, FAMILY_JOIN_REPLY)));
    }

    private void familyInfo(UserSession session, boolean withKeyboard) {
        Optional<FamilyData> optionalFamilyData =
                familyDataRepository.findById(session.getFamilyId());
        if (optionalFamilyData.isPresent()) {
            sendFamilyInfo(session, optionalFamilyData.get(), withKeyboard);
            log.info("Family info has been sent to user {}", session.getChatId());
        } else {
            log.warn("Family {} not found for user {}", session.getFamilyId(), session.getChatId());
            session.setFamilyId(null);
            session.userIsBusy();
            session.getPath().push(FAMILY_COMMAND);
            sendMessageService.sendMessage(session.getChatId(),
                    ERROR_FAMILY_ID_MESSAGE, session.getLocale());
            askFamilyId(session.getChatId(), session.getLocale());
        }
    }

    private void sendFamilyInfo(
            UserSession session, FamilyData familyData, boolean withKeyboard) {
        sendMessageService.sendMessage(session.getChatId(), FAMILY_INFO__FORMAT_MESSAGE,
                List.of(familyData.getFamilyName(),
                        familyData.getFamilyId() + SEPARATOR + familyData.getPassCode(),
                        getListOfCurrencies(familyData.getCurrency(), session.getLocale()),
                        String.valueOf(familyData.getChatIds().size())),
                session.getLocale(),
                getFamilyInfoKeyboard(session, withKeyboard, session.getLocale())
        );
    }

    private ReplyKeyboard getFamilyInfoKeyboard(
            UserSession session, boolean withKeyboard, Locale locale) {
        if (withKeyboard) {
            log.debug("Creating family menu keyboard for user {}", session.getChatId());
            return inlineCommandHelper.buildInlineKeyboard(locale,
                    List.of(
                            FAMILY_CHANGE_NAME_BUTTON,
                            FAMILY_CHANGE_CURRENCY_BUTTON,
                            FAMILY_MEMBERS_INFO_BUTTON,
                            FAMILY_LEAVE_BUTTON),
                    BACK_REMOVE_BUTTON);
        } else {
            if (isMainPage(session.getPath())) {
                log.debug("Creating family info with noRemoveReplyKeyboard for user {}", session.getChatId());
                session.getPath().clear();
                return new ReplyKeyboardRemove(false);
            } else {
                session.getPath().push(FAMILY_COMMAND);
                log.debug("Creating family info within the workflow for user {}", session.getChatId());
                return inlineCommandHelper
                        .buildInlineKeyboard(session.getLocale(), BACK_MODIFY_BUTTON);
            }
        }
    }

    private static boolean isMainPage(SessionPathDeque<String> path) {
        return path.isEmpty() || (path.size() == 1 && FAMILY_COMMAND.equals(path.peek()));
    }

    private static String getListOfCurrencies(Set<String> currencies, Locale locale) {
        if (currencies.isEmpty()) {
            return getMessage(NOT_AVAILABLE_MESSAGE, locale);
        }
        StringBuilder currencyList = new StringBuilder();
        for (String currency : currencies) {
            currencyList.append(currency).append(COMMA_SEPARATED_FORMAT);
        }
        currencyList.setLength(currencyList.length() - 2);
        return currencyList.toString();
    }

    private Long getValidFamilyId(String code, Long chatId) {
        if (code == null || chatId == null) {
            return null;
        }

        try {
            log.info("Validating family code {} for user {}", code, chatId);
            String[] passwordParts = code.split(SEPARATOR);
            if (passwordParts.length != 2) {
                log.warn("Invalid family code format: {}", code);
                return null;
            }

            Long familyId = Long.parseLong(passwordParts[0]);
            if (!FAMILY_IDS_SET.contains(familyId)) {
                log.warn("Family ID {} not found in FAMILY_IDS_SET", familyId);
                return null;
            }

            FamilyData family = familyDataRepository
                    .findByFamilyIdAndPassCode(familyId, passwordParts[1]);
            if (family == null) {
                log.warn("Family ID {} not found in the database", familyId);
                return null;
            }

            family.getChatIds().add(chatId);
            familyDataRepository.save(family);

            log.info("Family ID is valid and user {} added to the database", chatId);
            return familyId;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String getEncrypt(UserSession session, UserData member) {
        try {
            return encrypt(member.getChatId().toString(),
                    session.getChatId().toString());
        } catch (Exception e) {
            log.error("Error encrypting member ID: {}", e.getMessage());
            return member.getChatId().toString();
        }

    }

    private String createMemberDeleteLink(String code) {
        return String.format(BOT_INLINE_LINK__FORMAT,
                String.format(BOT_LINK__FORMAT, botName,
                        SEPARATOR2 + DELETE_MEMBER_SPECIAL_CODE + SEPARATOR2 + code),
                DELETE_MEMBER_LINK_SYMBOL
        );
    }

}