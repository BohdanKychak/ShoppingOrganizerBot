package org.example.helper.keyboard;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.example.service.support.PropertiesService.getInlineButton;

@Component
public class InlineCommandHelper {

    private static final int MAX_BUTTONS_ON_ROW = 3;
    private static final int MIN_BUTTONS_ON_ROW = 2;

    public InlineKeyboardMarkup buildInlineKeyboard(Locale locale, String key) {
        return new InlineKeyboardMarkup(List.of(fillRowWithSingleButton(locale, key)));
    }

    public InlineKeyboardMarkup buildInlineKeyboard(Locale locale, List<String> keys) {
        List<List<InlineKeyboardButton>> listRowInline = new ArrayList<>();

        if (keys.size() > 1) {
            fillRowWithButtonsMaxTwoInRow(locale, keys, null, listRowInline);
        } else {
            listRowInline.add(fillRowWithSingleButton(locale, keys.get(0)));
        }

        return new InlineKeyboardMarkup(listRowInline);
    }

    public InlineKeyboardMarkup buildInlineKeyboard(
            Locale locale, List<String> keys, String key) {
        List<List<InlineKeyboardButton>> listRowInline = new ArrayList<>();

        if (keys.size() > 1) {
            fillRowWithButtonsMaxTwoInRow(locale, keys, null, listRowInline);
        } else {
            listRowInline.add(fillRowWithSingleButton(locale, keys.get(0)));
        }

        listRowInline.add(fillRowWithSingleButton(locale, key));

        return new InlineKeyboardMarkup(listRowInline);
    }

    public InlineKeyboardMarkup buildInlineKeyboardWithoutLocale(List<String> buttons, List<String> values) {
        List<List<InlineKeyboardButton>> listRowInline = new ArrayList<>();

        if (buttons.size() == values.size()) {
            fillRowWithButtonsMaxTwoInRow(null, buttons, values, listRowInline);
        } else {
            fillRowWithButtonsMaxTwoInRow(null, buttons, buttons, listRowInline);
        }

        return new InlineKeyboardMarkup(listRowInline);
    }

    public InlineKeyboardMarkup buildInlineKeyboardMaxThreeButton(
            Locale locale,
            List<String> keys, List<String> values,
            String key1, String key2
    ) {
        List<List<InlineKeyboardButton>> listRowInline = new ArrayList<>();

        if (keys.size() > 1) {
            fillRowWithButtonsMaxThreeInRow(null, keys, values, listRowInline);
        } else {
            listRowInline.add(fillRowWithSingleButton(null, keys.get(0)));
        }

        if (key1 != null) {
            listRowInline.add(fillRowWithSingleButton(locale, key1));
        }
        if (key2 != null) {
            listRowInline.add(fillRowWithSingleButton(locale, key2));
        }

        return new InlineKeyboardMarkup(listRowInline);
    }

    public InlineKeyboardMarkup buildInlineKeyboardMaxThreeButton(
            Locale locale, List<String> keys) {
        List<List<InlineKeyboardButton>> listRowInline = new ArrayList<>();

        if (keys.size() > 1) {
            fillRowWithButtonsMaxThreeInRow(locale, keys, null, listRowInline);
        } else {
            listRowInline.add(fillRowWithSingleButton(locale, keys.get(0)));
        }

        return new InlineKeyboardMarkup(listRowInline);
    }

    public InlineKeyboardMarkup buildInlineKeyboardForPurchaseLast(
            Locale locale, List<String> firstLine,
            List<String> secondLine, List<String> thirdLine
    ) {
        List<List<InlineKeyboardButton>> listRowInline = new ArrayList<>();

        listRowInline.add(fillRowWithAnyButton(locale, firstLine));
        listRowInline.add(fillRowWithAnyButton(locale, secondLine));
        listRowInline.add(fillRowWithAnyButton(locale, thirdLine));

        return new InlineKeyboardMarkup(listRowInline);
    }

    private static void fillRowWithButtonsMaxTwoInRow(
            Locale locale, List<String> keys, List<String> values,
            List<List<InlineKeyboardButton>> listRowInline) {
        fillRowsWithTwoButton(locale, keys, values, listRowInline);
    }

    private static void fillRowWithButtonsMaxThreeInRow(
            Locale locale, List<String> keys, List<String> values,
            List<List<InlineKeyboardButton>> listRowInline) {
        fillRowsWithTwoButton(locale, keys, values, listRowInline,
                fillRowsWithThreeButton(locale, keys, values, listRowInline));
    }

    private static List<InlineKeyboardButton> fillRowWithSingleButton(
            Locale locale, String key) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText(getButtonText(key, locale));
        inlineKeyboardButton.setCallbackData(key);
        return List.of(inlineKeyboardButton);
    }

    private static int fillRowsWithThreeButton(
            Locale locale,
            List<String> keys,
            List<String> values,
            List<List<InlineKeyboardButton>> listRowInline) {
        if (values == null || values.size() != keys.size()) {
            values = keys;
        }

        int indexForThreeButton = 0;

        // get number of rows with 3 buttons (without 2 button rows)
        int numberOfTripleRows = getNumberOfTripleRows(keys);

        // fill rows with 3 buttons
        List<InlineKeyboardButton> rowInline;
        for (int j = 0; j < numberOfTripleRows; j++) {
            rowInline = new ArrayList<>();
            for (int i = 0; i < MAX_BUTTONS_ON_ROW; i++, indexForThreeButton++) {
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(getButtonText(keys.get(indexForThreeButton), locale));
                inlineKeyboardButton.setCallbackData(values.get(indexForThreeButton));
                rowInline.add(inlineKeyboardButton);
            }
            listRowInline.add(rowInline);
        }
        return indexForThreeButton; // return index for 2 button rows
    }

    private static void fillRowsWithTwoButton(
            Locale locale,
            List<String> keys,
            List<String> values,
            List<List<InlineKeyboardButton>> listRowInline) {
        fillRowsWithTwoButton(locale, keys, values, listRowInline, 0);
    }

    private static void fillRowsWithTwoButton(
            Locale locale,
            List<String> keys,
            List<String> values,
            List<List<InlineKeyboardButton>> listRowInline,
            int indexForTwoButton) {
        if (values == null || values.size() != keys.size()) {
            values = keys;
        }

        // fill row with 2 buttons
        List<InlineKeyboardButton> rowInline;
        while (indexForTwoButton < keys.size()) {
            rowInline = new ArrayList<>();
            for (int i = 0; i < MIN_BUTTONS_ON_ROW
                    && indexForTwoButton < keys.size(); i++, indexForTwoButton++) {
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(getButtonText(keys.get(indexForTwoButton), locale));
                inlineKeyboardButton.setCallbackData(values.get(indexForTwoButton));
                rowInline.add(inlineKeyboardButton);
            }
            listRowInline.add(rowInline);
        }
    }

    private static List<InlineKeyboardButton> fillRowWithAnyButton(
            Locale locale, List<String> keys) {
        List<InlineKeyboardButton> keyboardButtons = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(getButtonText(key, locale));
                inlineKeyboardButton.setCallbackData(key);
                keyboardButtons.add(inlineKeyboardButton);
            }
        }
        return keyboardButtons;
    }

    private static String getButtonText(String key, Locale locale) {
        return locale != null ? getInlineButton(key, locale) : key;
    }

    private static int getNumberOfTripleRows(List<String> keys) {
        return keys.size() / MAX_BUTTONS_ON_ROW - (keys.size() % MAX_BUTTONS_ON_ROW == 1 ? 1 : 0);
    }
}