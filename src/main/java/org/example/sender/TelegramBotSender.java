package org.example.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Component
public class TelegramBotSender extends DefaultAbsSender {

    @SuppressWarnings("unused")
    @Value("${telegram.bot.token}")
    private String botToken;

    @SuppressWarnings("deprecation")
    protected TelegramBotSender() {
        super(new DefaultBotOptions());
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getBotToken() {
        return botToken;
    }
}