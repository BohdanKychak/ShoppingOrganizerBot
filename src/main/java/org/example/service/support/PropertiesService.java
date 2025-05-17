package org.example.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.example.util.Constants.PATH_TO_LANG_BUTTONS;
import static org.example.util.Constants.PATH_TO_LANG_MESSAGES;
import static org.example.util.Constants.PATH_TO_LANG_REPLY_BUTTONS;

@Slf4j
@Component
public class PropertiesService {

    private static final Map<Locale, ResourceBundle> resourceBundleCacheForMessage = new HashMap<>();
    private static final Map<Locale, ResourceBundle> resourceBundleCacheForInlineButton = new HashMap<>();
    private static final Map<Locale, ResourceBundle> resourceBundleCacheForReplyButton = new HashMap<>();
    private static final Map<Locale, Map<String, String>> reverseMapCacheForReplyButton = new HashMap<>();

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    private static final String PRIORITY_LOCALE = "uk";
    private static final String LOCALE_TO_CONVERT = "ru";

    public static String getMessage(String key, Locale locale) {
        if (locale == null) {
            return key;
        }
        ResourceBundle messages =
                resourceBundleCacheForMessage.computeIfAbsent(locale,
                        loc -> ResourceBundle.getBundle(
                                PATH_TO_LANG_MESSAGES, loc)
                );
        if (!messages.containsKey(key)) {
            return key;
        }
        return messages.getString(key);
    }

    public static String getMessage(String key, String localeString) {
        Locale locale = getLocale(localeString);
        return getMessage(key, locale);
    }

    public static String getInlineButton(String key, Locale locale) {
        if (locale == null) {
            return key;
        }
        ResourceBundle buttons =
                resourceBundleCacheForInlineButton.computeIfAbsent(locale,
                        loc -> ResourceBundle.getBundle(
                                PATH_TO_LANG_BUTTONS, loc)
                );
        if (!buttons.containsKey(key)) {
            return key;
        }
        return buttons.getString(key);
    }

    public static String getReplyButton(String key, Locale locale) {
        if (locale == null) {
            return key;
        }
        ResourceBundle buttons =
                resourceBundleCacheForReplyButton.computeIfAbsent(locale,
                        loc -> ResourceBundle.getBundle(
                                PATH_TO_LANG_REPLY_BUTTONS, loc)
                );
        if (!buttons.containsKey(key)) {
            return key;
        }
        return buttons.getString(key);
    }

    public static String getButtonCodeByText(String text, Locale locale) {
        if (!reverseMapCacheForReplyButton.containsKey(locale)) {
            buildReverseMapReplyButtons(locale);
        }
        String code = reverseMapCacheForReplyButton.get(locale).get(text.toLowerCase());
        if (code == null && !locale.equals(DEFAULT_LOCALE)) {
            if (!reverseMapCacheForReplyButton.containsKey(DEFAULT_LOCALE)) {
                buildReverseMapReplyButtons(DEFAULT_LOCALE);
            }
            code = reverseMapCacheForReplyButton.get(DEFAULT_LOCALE).get(text.toLowerCase());
        }
        return code;
    }

    public static Locale getLocale(String locale) {
        return locale == null ? DEFAULT_LOCALE
                : Locale.forLanguageTag(
                locale.equals(LOCALE_TO_CONVERT) ? PRIORITY_LOCALE : locale);
    }

    public static String getFullText(String str, List<String> list) {
        return list == null || list.isEmpty() ? str : String.format(str, list.toArray(new Object[0]));
    }

    private static void buildReverseMapReplyButtons(Locale locale) {
        ResourceBundle messages =
                resourceBundleCacheForReplyButton.computeIfAbsent(locale,
                        loc -> ResourceBundle.getBundle(
                                PATH_TO_LANG_REPLY_BUTTONS, loc)
                );
        Map<String, String> reverseMap = new HashMap<>();
        for (String key : messages.keySet()) {
            reverseMap.put(messages.getString(key).toLowerCase(), key.toLowerCase());
        }
        reverseMapCacheForReplyButton.put(locale, reverseMap);
    }
}
