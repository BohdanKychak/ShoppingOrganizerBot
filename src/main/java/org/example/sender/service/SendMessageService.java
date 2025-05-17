package org.example.sender.service;

import lombok.extern.slf4j.Slf4j;
import org.example.model.entity.UserData;
import org.example.sender.TelegramBotSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import static org.example.action.tag.InlineButtonKey.BACK_REMOVE_BUTTON;
import static org.example.bot.Dispatcher.USER_SESSION_MAP;
import static org.example.service.support.PropertiesService.getFullText;
import static org.example.service.support.PropertiesService.getInlineButton;
import static org.example.service.support.PropertiesService.getMessage;
import static org.example.util.Constants.LIFETIME_IMPORTANT_MESSAGE;
import static org.example.util.MessageProperties.MESSAGE_EXPIRED_MESSAGE;

@Slf4j
@Component
public class SendMessageService {

    @Autowired
    private UpdateMessageService updateMessageService;

    private final TelegramBotSender botSender;

    public SendMessageService(TelegramBotSender botSender) {
        this.botSender = botSender;
    }

    // message without keyboard and not to delete previous keyboard
    // without locale and format parameters
    public void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = createMessage(chatId, text);
        send(chatId, sendMessage);
    }

    // message without keyboard and delete previous keyboard
    // without locale and format parameters
    public void sendMessage(Long chatId, String text, boolean removeKeyboard) {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(removeKeyboard);
        SendMessage sendMessage = createMessage(chatId, text, keyboardRemove);
        send(chatId, sendMessage);
    }

    // message with KeyboardMarkup (ReplyKeyboardMarkup or InlineKeyboardMarkup)
    // with locale and without format parameters
    public void sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = createMessage(chatId, text, replyKeyboard);
        send(chatId, sendMessage);
    }

    // message without keyboard and delete previous keyboard
    // with locale and without format parameters
    public void sendMessage(Long chatId, String key, Locale locale) {
        sendMessage(chatId, key, locale, true);
    }

    // message without keyboard and delete previous keyboard
    // with locale and without format parameters
    public void sendMessage(
            Long chatId, String key, Locale locale, boolean removeKeyboard
    ) {
        String text = getMessage(key, locale);
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(removeKeyboard);
        SendMessage sendMessage = createMessage(chatId, text, keyboardRemove);
        send(chatId, sendMessage);
    }

    // message with KeyboardMarkup (ReplyKeyboardMarkup or InlineKeyboardMarkup)
    // with locale and without format parameters
    public void sendMessage(
            Long chatId, String key, Locale locale, ReplyKeyboard replyKeyboard
    ) {
        String text = getMessage(key, locale);
        SendMessage sendMessage = createMessage(chatId, text, replyKeyboard);
        send(chatId, sendMessage);
    }

    // message without keyboard and delete previous keyboard
    // with locale and format parameters
    public void sendMessage(
            Long chatId, String key, List<String> params, Locale locale
    ) {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        String text = getFullText(getMessage(key, locale), params);
        SendMessage sendMessage = createMessage(chatId, text, keyboardRemove);
        send(chatId, sendMessage);
    }

    // message with KeyboardMarkup (ReplyKeyboardMarkup or InlineKeyboardMarkup)
    // with locale and format parameters
    public void sendMessage(
            Long chatId, String key, List<String> params,
            Locale locale, ReplyKeyboard replyKeyboard
    ) {
        String text = getFullText(getMessage(key, locale), params);
        SendMessage sendMessage = createMessage(chatId, text, replyKeyboard);
        send(chatId, sendMessage);
    }

    // temporary message with delete KeyboardMarkup
    // with locale and without format parameters
    public void sendTemporaryMessage(Long chatId, String key, Locale locale) {
        String text = getMessage(key, locale);
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(true);
        SendMessage sendMessage = createMessage(chatId, text, keyboardRemove);
        Message message = send(chatId, sendMessage);
        updateMessageService.deleteMessage(chatId, message.getMessageId());
    }

    public void sendMessageWithExpirationDate(
            Long chatId, String key, List<String> params,
            Locale locale, ReplyKeyboard replyKeyboard
    ) {
        String text = getFullText(getMessage(key, locale), params);
        SendMessage sendMessage = createMessage(chatId, text, replyKeyboard);
        Message message = send(chatId, sendMessage);
        scheduleMessageDeletion(chatId, message.getMessageId(),
                locale, message.getReplyMarkup());
    }

    private void scheduleMessageDeletion(Long chatId, Integer messageId,
                                         Locale locale, ReplyKeyboard replyKeyboard) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateMessageService.editInlineMessage(chatId,
                        messageId, locale, MESSAGE_EXPIRED_MESSAGE,
                        getBackButton(locale)
                );
            }
        }, LIFETIME_IMPORTANT_MESSAGE);
    }

    private static InlineKeyboardMarkup getBackButton(Locale locale) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(getInlineButton(BACK_REMOVE_BUTTON, locale));
        backButton.setCallbackData(BACK_REMOVE_BUTTON);
        keyboard.setKeyboard(Collections
                .singletonList(Collections.singletonList(backButton)));
        return keyboard;
    }

    // message for some users and not remove KeyboardMarkup & without save messageId
    // with locale and with format parameters
    public void sendMessageForSomeUsersNotSaveMessage(
            List<UserData> users, String key, List<String> params) {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(false);
        for (UserData user : users) {
            sendWithoutSave(user.getChatId(),
                    createMessage(
                            user.getChatId(),
                            getFullText(getMessage(key, user.getLocale()), params),
                            keyboardRemove
                    )
            );
        }
    }

    public void sendMessageNotSaveMessage(Long chatId, String key, Locale locale) {
        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
        keyboardRemove.setRemoveKeyboard(false);

        sendWithoutSave(chatId, createMessage(chatId,
                locale != null ? getMessage(key, locale) : key, keyboardRemove));
    }

    public void sendMessageNotSaveMessage(
            Long chatId, String key, Locale locale, ReplyKeyboard replyKeyboard) {
        sendWithoutSave(chatId, createMessage(chatId,
                getMessage(key, locale), replyKeyboard));
    }

    public Message sendMessageNotSaveMessage(
            Long chatId, String button, ReplyKeyboard replyKeyboard) {
        return sendWithoutSave(chatId, createMessage(chatId, button, replyKeyboard));
    }

    private static SendMessage createMessage(Long chatId, String text) {
        SendMessage sendMessage = SendMessage
                .builder()
                .text(text)
                .chatId(chatId.toString())
                .parseMode(ParseMode.HTML)
                .build();
        sendMessage.enableHtml(true);
        return sendMessage;
    }

    private static SendMessage createMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = SendMessage
                .builder()
                .text(text)
                .chatId(chatId.toString())
                .replyMarkup(replyKeyboard)
                .parseMode(ParseMode.HTML)
                .build();
        sendMessage.enableHtml(true);
        return sendMessage;
    }

    private Message send(Long chatId, SendMessage sendMessage) {
        Message message = Optional
                .ofNullable(send(sendMessage))
                .map(sentMessage -> {
                    log.debug("Send message to chatId {}", chatId);
                    return sentMessage;
                })
                .orElse(null);

        if (message != null && USER_SESSION_MAP.containsKey(chatId))
            USER_SESSION_MAP.get(chatId).setLastMessageId(message.getMessageId());

        return message;
    }

    private Message sendWithoutSave(Long chatId, SendMessage sendMessage) {
        return Optional.ofNullable(send(sendMessage))
                .map(sentMessage -> {
                    log.debug("Send message without save to chatId {}", chatId);
                    return sentMessage;
                })
                .orElse(null);
    }

    private Message send(SendMessage sendMessage) {
        try {
            return botSender.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Failed to send message {}", e.getMessage());
            return null;
        }
    }
}
