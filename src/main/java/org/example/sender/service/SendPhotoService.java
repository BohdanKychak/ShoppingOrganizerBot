package org.example.sender.service;

import lombok.extern.slf4j.Slf4j;
import org.example.exception.PictureNotAvailableException;
import org.example.sender.TelegramBotSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class SendPhotoService {

    private final TelegramBotSender botSender;

    public SendPhotoService(TelegramBotSender botSender) {
        this.botSender = botSender;
    }

    public void sendPhoto(String chatId, String text, String photo) throws PictureNotAvailableException {
        SendPhoto sendPhoto = new SendPhoto();

        sendPhoto.setPhoto(new InputFile(photo));
        sendPhoto.setCaption(text);
        sendPhoto.setChatId(chatId);
        sendPhoto.setParseMode(ParseMode.HTML);

        log.debug("Send picture: \"{}\" to {}", photo, chatId);
        send(sendPhoto);
    }

    private void send(SendPhoto sendPhoto) throws PictureNotAvailableException {
        try {
            botSender.execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("Failed to send picture {}", e.getMessage());
            throw new PictureNotAvailableException();
        }
    }
}
