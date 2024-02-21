package az.code.turaltelegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import az.code.turaltelegrambot.entity.Session;

import java.util.UUID;

public interface SessionRepo extends JpaRepository<Session, UUID> {

}
