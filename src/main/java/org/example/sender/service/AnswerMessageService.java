package org.example.sender.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sender.TelegramBotSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;

import static org.example.service.support.PropertiesService.getMessage;

@Slf4j
@Component
public class AnswerMessageService {

    private static final int MAX_LENGTH = 200;

    private final TelegramBotSender botSender;

    public AnswerMessageService(TelegramBotSender botSender) {
        this.botSender = botSender;
    }

    public void sendBigWindowAnswer(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        answer.setText(getTextForWindow(text));
        answer.setShowAlert(true);
        log.debug("Send big window answer by callbackId {}", callbackId);
        send(answer);
    }

    public void sendSmallWindowAnswer(String callbackId, String code, Locale locale) {
        sendSmallWindowAnswer(callbackId, getMessage(code, locale));
    }

    public void sendSmallWindowAnswer(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        answer.setText(text);
        answer.setShowAlert(false);
        log.debug("Send small window answer by callbackId {}", callbackId);
        send(answer);
    }

    private void send(AnswerCallbackQuery answerCallbackQuery) {
        try {
            botSender.execute(answerCallbackQuery);
        } catch (TelegramApiException e) {
            log.error("Failed to send answer {}", e.getMessage());
        }
    }

    private String getTextForWindow(String text) {
        if (text == null) {
            return "null";
        }
        return text.length() > MAX_LENGTH
                ? text.substring(0, MAX_LENGTH)
                : text;
    }
}
