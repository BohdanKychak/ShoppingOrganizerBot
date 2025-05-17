package org.example.helper.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.example.service.support.PropertiesService.getReplyButton;

@Component
public class ReplyKeyboardHelper {

    public ReplyKeyboardMarkup buildKeyboardOnePerLine(
            Locale locale, List<String> keyboards
    ) {
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        for (String keyboard : keyboards) {
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(getReplyButton(keyboard, locale));
            keyboardRows.add(keyboardRow);
        }

        return buttonBuilder(keyboardRows);
    }

    public ReplyKeyboardMarkup buildKeyboardTwoPerLine(
            Locale locale, List<String> keyboards
    ) {
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        for (int i = 0; i < keyboards.size(); i += 2) {
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(getReplyButton(keyboards.get(i), locale));
            if (i + 1 < keyboards.size())
                keyboardRow.add(getReplyButton(keyboards.get(i + 1), locale));
            keyboardRows.add(keyboardRow);
        }

        return buttonBuilder(keyboardRows);
    }

    private ReplyKeyboardMarkup buttonBuilder(List<KeyboardRow> rows) {
        return ReplyKeyboardMarkup.builder()
                .keyboard(rows)
                .selective(true)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }
}
