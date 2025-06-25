package org.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfiguration {

    public static final String WEBHOOK = "webhook";

    @Bean
    public SetWebhook setWebhook(TelegramBotConfig config) {
        return SetWebhook.builder()
                .url(config.getDomain() + config.getPath())
                .build();
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(
            TelegramBotConfig config,
            @Lazy @Autowired(required = false) TelegramLongPollingBot pollingBot,
            @Lazy @Autowired(required = false) TelegramWebhookBot webhookBot,
            @Lazy @Autowired(required = false) SetWebhook setWebhook
    ) throws TelegramApiException {

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        if (WEBHOOK.equalsIgnoreCase(config.getMode())
                && webhookBot != null && setWebhook != null) {
            botsApi.registerBot(webhookBot, setWebhook);
        } else if (pollingBot != null) {
            botsApi.registerBot(pollingBot);
        } else {
            throw new IllegalStateException("No valid bot configuration found");
        }

        return botsApi;
    }

}


