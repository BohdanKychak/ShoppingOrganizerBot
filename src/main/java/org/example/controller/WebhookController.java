package org.example.controller;

import org.example.bot.TelegramWebhookBot;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

    private final TelegramWebhookBot telegramWebhookBot;

    public WebhookController(TelegramWebhookBot telegramWebhookBot) {
        this.telegramWebhookBot = telegramWebhookBot;
    }

    @PostMapping("${telegram.bot.path}")
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return telegramWebhookBot.onWebhookUpdateReceived(update);
    }
}

