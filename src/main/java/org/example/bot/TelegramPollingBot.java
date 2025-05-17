package org.example.bot;

import lombok.extern.slf4j.Slf4j;
import org.example.config.TelegramBotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@SuppressWarnings("deprecation")
public class TelegramPollingBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final Dispatcher dispatcher;

    public TelegramPollingBot(TelegramBotConfig config, Dispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            dispatcher.messageResponse(update);
        } else if (update.hasCallbackQuery()) {
            dispatcher.callbackQueryResponse(update);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }
}