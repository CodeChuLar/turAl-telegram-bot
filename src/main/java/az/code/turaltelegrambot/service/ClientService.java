package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Client;

import java.util.Optional;

public interface ClientService {
    Optional<Client> getByChatId(long chatId);
    Optional<Client> getById(long id);
    Client create(Client client);
    Client save(Client client);
    void delete(long id);
}
