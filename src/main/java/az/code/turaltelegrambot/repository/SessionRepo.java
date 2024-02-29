package az.code.turaltelegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import az.code.turaltelegrambot.entity.Session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepo extends JpaRepository<Session, UUID> {
    List<Session> findByClient_ChatId(long chatId);
    List<Session> findAllByClient_ChatId(long chatId);
}
