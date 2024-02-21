package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Session;
import az.code.turaltelegrambot.repository.SessionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    private final SessionRepo sessionRepo;
    @Override
    public Session get(UUID uuid) {
        return sessionRepo.getReferenceById(uuid);
    }

    @Override
    public Session create(Session session) {
        return sessionRepo.save(session);
    }

    @Override
    public Session update(UUID uuid, Session session) {
        Session updatingSession = sessionRepo.getReferenceById(uuid);
        sessionRepo.delete(updatingSession);
        updatingSession=session;
        return sessionRepo.save(updatingSession);
    }

    @Override
    public void delete(UUID id) {
        sessionRepo.deleteById(id);
    }
}
