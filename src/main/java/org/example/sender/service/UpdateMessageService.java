package org.example.sender.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sender.TelegramBotSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodSerializable;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.Locale;

import static org.example.service.support.PropertiesService.getMessage;

@Slf4j
@Component
public class UpdateMessageService {

    private final TelegramBotSender botSender;

    public UpdateMessageService(TelegramBotSender botSender) {
        this.botSender = botSender;
    }

    public void updateKeyboard(
            Long chatId, Integer messageId, InlineKeyboardMarkup newKeyboard) {
        if (isIdsNull(chatId, messageId)) return;

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(chatId);
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(newKeyboard);

        log.debug("Update keyboard in messageId {} in chatId {}", messageId, chatId);
        update(editMarkup);
    }

    public void editInlineMessage(
            Long chatId, Integer messageId, Locale locale, String newText) {
        editInlineMessage(chatId, messageId, locale, newText, null);
    }

    public void editInlineMessage(
            Long chatId, Integer messageId, Locale locale,
            String newText, InlineKeyboardMarkup inlineKeyboard
    ) {
        if (isIdsNull(chatId, messageId)) return;

        EditMessageText newMessage = new EditMessageText();
        newMessage.setChatId(String.valueOf(chatId));
        newMessage.setMessageId(messageId);
        newMessage.setText(locale == null ? newText : getMessage(newText, locale));
        newMessage.setReplyMarkup(inlineKeyboard);
        newMessage.enableHtml(true);

        log.debug("Edit message {} in chatId {}", messageId, chatId);
        update(newMessage);
    }

    private void update(BotApiMethodSerializable editMarkup) {
        try {
            botSender.execute(editMarkup);
        } catch (TelegramApiException e) {
            log.warn("Failed to update message {}", e.getMessage());
        }
    }

    // can delete it only for 48 hours
    public void deleteMessage(Long chatId, Integer messageId) {
        if (messageId == null) {
            log.warn("Message didn't delete from chat {} because messageId is null", chatId);
            return;
        }
        try {
            botSender.execute(DeleteMessage.builder().chatId(chatId.toString()).messageId(messageId).build());
        } catch (TelegramApiRequestException e) {
            if (e.getApiResponse().equals("Bad Request: message can't be deleted for everyone")) {
                updateKeyboard(chatId, messageId, null);
                log.debug("Keyboard deleted message {}", e.getMessage());
                return;
            }
            if (e.getApiResponse().equals("Bad Request: message to delete not found")) {
                log.info("Message {} to delete not found", messageId);
                return;
            }
            log.warn("Failed to delete message (API issue): {}", e.getMessage());
        } catch (TelegramApiException e) {
            log.warn("Failed to delete message (General error): {}", e.getMessage());
        }
    }

    private boolean isIdsNull(Long chatId, Integer messageId) {
        if (chatId == null) {
            log.warn("Message didn't update because chatId is null");
            return true;
        }
        if (messageId == null) {
            log.warn("Message didn't update in chat {} because messageId is null", chatId);
            return true;
        }
        return false;
    }
}
