package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram.bot")
@Data
public class TelegramBotConfig {
    private String token;
    private String username;
    private String path; // webhook path
    private String mode; // "webhook" or "polling"
}

