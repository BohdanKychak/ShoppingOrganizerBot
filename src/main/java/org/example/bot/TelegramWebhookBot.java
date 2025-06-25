package org.example.bot;

import lombok.extern.slf4j.Slf4j;
import org.example.config.TelegramBotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class TelegramWebhookBot extends org.telegram.telegrambots.bots.TelegramWebhookBot {

    private final TelegramBotConfig config;
    private final Dispatcher dispatcher;

    public TelegramWebhookBot(TelegramBotConfig config, Dispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            dispatcher.messageResponse(update);
        } else if (update.hasCallbackQuery()) {
            dispatcher.callbackQueryResponse(update);
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotPath() {
        return config.getPath();
    }
}