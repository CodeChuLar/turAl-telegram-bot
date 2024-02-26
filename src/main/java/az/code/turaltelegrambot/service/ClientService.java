package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Client;

import java.util.Optional;

public interface ClientService {
    Optional<Client> getByChatId(long chatId);
    Client create(Client client);
    void delete(long id);
}
