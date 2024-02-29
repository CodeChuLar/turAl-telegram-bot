package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Session;
import az.code.turaltelegrambot.repository.SessionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
    public List<Session> allSessionsByChatId(long chatId) {
        return sessionRepo.findAllByClient_ChatId(chatId);
    }

    public List<Session> getActiveSessions(long chatId){
        return sessionRepo.findByClient_ChatId(chatId)
                .stream()
                .filter(Session::isActive)
                .toList();
    }

    @Override
    public Session create(Session session) {
        return sessionRepo.save(session);
    }

    @Override
    public Session update(UUID uuid, Session updatedSession) {
        Optional<Session> optionalSession = sessionRepo.findById(uuid);
        if (optionalSession.isPresent()) {
            Session updatingSession = optionalSession.get();
            updatingSession.setActive(updatedSession.isActive());
            return sessionRepo.save(updatingSession);
        } else return null;
    }

    @Override
    public void delete(UUID id) {
        sessionRepo.deleteById(id);
    }
}
