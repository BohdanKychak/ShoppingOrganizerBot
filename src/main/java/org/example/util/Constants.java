package org.example.util;

import java.time.format.DateTimeFormatter;

public final class Constants {

    public static final String BOT_TAG_FORMAT = "@%s";

    public static final String PATH_TO_LANG_PROPERTIES = "lang";
    public static final String PATH_TO_LANG_MESSAGES = PATH_TO_LANG_PROPERTIES + "/messages/messages";
    public static final String PATH_TO_LANG_BUTTONS = PATH_TO_LANG_PROPERTIES + "/buttons/buttons";
    public static final String PATH_TO_LANG_REPLY_BUTTONS = PATH_TO_LANG_PROPERTIES + "/buttons/reply-buttons";

    public static final String AMOUNT_FORMAT = "%.2f";
    public static final String CURRENCY_CONVERT__FORMAT = "%.2f %s -> %.2f %s";
    public static final String CURRENCY_LIST__FORMAT = "<u>%s</u> -> <b>%s:</b> <i>%s, %s</i>";
    public static final String ACTIVE = "\uD83D\uDD33";
    public static final String INACTIVE = "\uD83D\uDD32";
    public static final String AVAILABLE = "\uD83D\uDD33";
    public static final String UNAVAILABLE = "\uD83D\uDD32";
    public static final String LANGUAGE_STRING_LIST_FORMAT = "\n %s - <i>%s</i>";

    public static final int ZERO = 0;
    public static final String EMPTY = "";
    public static final String SEPARATOR = "-";
    public static final String SEPARATOR2 = "_";
    public static final String SEPARATOR3 = "\\.";
    public static final String COMMAND_SYMBOL = "/";
    public static final String NEW_LINE = "\n";
    public static final String COMMA_SEPARATED_FORMAT = ", ";
    public static final String SPACE = " ";
    public static final String POINT = ".";
    public static final String COMA = ",";
    public static final String IGNORE_SYMBOL = "\\.";
    public static final String ELLIPSIS = "...";
    public static final String PLUS = "+";
    public static final String MINUS = "-";

    public static final String FAMILY_START_NAME_TEMPLATE = "Family";
    public static final String FAMILY_MEMBER_TEMPLATE = "     %d. <i>%s</i>";
    public static final String FAMILY_CODES__FORMAT = "-> <code>%s</code>\n";
    public static final String DELETE_MEMBER_SPECIAL_CODE = "delete-member-code";
    public static final String FAMILY_CREATE_CAPTCHA = "family.create.captcha";
    public static final String DELETE_MEMBER_LINK_SYMBOL = "\uD83D\uDCA2";
    public static final Long FIRST_FAMILY_ID = 1835049627L;
    public static final int FAMILY_PASS_LENGTH = 6;
    public static final int FAMILY_MAX_LENGTH_NAME = 20;
    public static final int FAMILY_MIN_LENGTH_NAME = 3;
    public static final int FAMILIES_MAX_LIMIT = 10;

    public static final int MAX_AMOUNT_CURRENCY_TEST = 1_000_000_000;
    public static final Integer MAX_DESCRIPTION_LENGTH = 200;
    public static final int DESCRIPTION_INFO_LENGTH = 70;
    public static final int TRANSACTIONS_LIST_LIMIT = 150;
    public static final Integer TRANSACTIONS_TABLE_LIMIT = 10;
    public static final Integer TRANSACTIONS_SORTED_TABLE_LIMIT = 15;

    public static final int LIFETIME_IMPORTANT_MESSAGE = 10 * 60 * 1000; // Minute 10

    public static final String MONEY_TRANSACTION_PATTERN = "(\\d+\\.?,?\\d{0,2})(\\s*)?([€$₣₴]|[A-Za-zŁłА-Яа-я]+)";
    public static final String MONEY_TRANSACTION_DESCRIPTION_PATTERN = "(\\d+\\.?,?\\d{0,2})(\\s*)?([€$₣₴]|[A-Za-zŁłА-Яа-я]+)((\\s*-\\s*)(.*))?";

    public static final String SYMBOLS_FOR_PASS_GENERATE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static final DateTimeFormatter ONE_DAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter ONE_MONTH_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM.yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd-MM");
    public static final DateTimeFormatter DATE_TIME_FORMATTER_FULL =
            DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy");

    public static final String BOT_LINK__FORMAT = "https://t.me/%s?start=%s";
    public static final String BOT_INLINE_LINK__FORMAT = "<a href='%s'>%s</a>";

    public static final String PURCHASE_TABLE_DATA__FORMAT = "%s <code>%9s</code> | \t%5s\t\t\t | %5s\t\t | %-12s\n";
    public static final String PURCHASE_TABLE_LINK_SYMBOL = "\uD83D\uDD17";
    public static final String PURCHASE_TABLE_FILE_SYMBOL = "\uD83D\uDCC2";
    public static final String HIDDEN_PRICE = "#####";
    public static final String PURCHASE_LAST_SPECIAL_CODE = "purchase-last-code";
    public static final String PURCHASE_SORTED_SPECIAL_CODE = "purchase-sorted-code";
    public static final String PURCHASE_GENERATE_PATH_CODE = "purchase-generate-path-code";
    public static final String DESCRIPTION_GIFT = "description.gift";

}
