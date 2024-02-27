package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Session;

import java.util.Optional;
import java.util.UUID;

public interface SessionService {
    Session get(UUID uuid);
    Session create(Session session);
    Optional<Session> getByChatId(long chatId);
    Session update(UUID id, Session session);
    void delete(UUID id);
}
