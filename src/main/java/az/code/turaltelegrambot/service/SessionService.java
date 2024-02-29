package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Session;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    Session get(UUID uuid);
    List<Session> allSessionsByChatId(long chatId);
    Session create(Session session);
    List<Session> getActiveSessions(long chatId);
    Session update(UUID id, Session session);
    void delete(UUID id);
}
