package org.example.model.session;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.model.helper.SessionPathDeque;
import org.example.model.entity.UserData;
import org.example.service.support.PropertiesService;

import java.util.Locale;

@Getter
@NoArgsConstructor
public class UserSession {

    private Long chatId;

    private String name;

    @Setter
    private Locale locale;

    @Setter
    private Long familyId;

    private boolean isBusy;

    private SessionPathDeque<String> path;

    private TransactionSession transactionSession;

    @Setter
    private Integer lastMessageId;

    @Setter
    private String temporaryData;

    public void userIsBusy() {
        isBusy = true;
    }

    public void userIsFree() {
        isBusy = false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long chatId;
        private String name;
        private Locale locale;

        public Builder chatId(Long chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public UserSession build() {
            UserSession session = new UserSession();
            session.chatId = this.chatId;
            session.name = this.name;
            session.locale = this.locale;
            session.isBusy = false;
            session.path = new SessionPathDeque<>();
            session.transactionSession = new TransactionSession();
            session.lastMessageId = 1;
            return session;
        }
    }

    public static UserSession createUserSessionByUserData(@NotNull UserData userData) {
        UserSession userSession = UserSession.builder()
                .chatId(userData.getChatId())
                .name(userData.getUserName())
                .locale(PropertiesService.getLocale(userData.getLocale()))
                .build();
        userSession.setFamilyId(userData.getFamilyId());
        return userSession;
    }

    public void clearAllTemporaryData() {
        this.temporaryData = null;
        this.path.clear();
        this.transactionSession.clearPurchaseCreation();
        this.transactionSession.clearPurchaseLastHistory();
    }
}


