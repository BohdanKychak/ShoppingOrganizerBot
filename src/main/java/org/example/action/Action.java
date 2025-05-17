package org.example.action;

import org.example.model.session.UserSession;
import org.telegram.telegrambots.meta.api.objects.Update;

@FunctionalInterface
public interface Action {
    void execute(UserSession session, Update update);
}
