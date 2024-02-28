package az.code.turaltelegrambot.services;

import az.code.turaltelegrambot.entity.Client;
import az.code.turaltelegrambot.repository.ClientRepo;
import az.code.turaltelegrambot.service.ClientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepo clientRepo;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    void getByChatId_ExistingChatId_ReturnsOptionalClient() {

        long chatId = 123;
        Client expectedClient = new Client(/* initialize with necessary values */);
        when(clientRepo.getClientByChatId(chatId)).thenReturn(Optional.of(expectedClient));


        Optional<Client> result = clientService.getByChatId(chatId);


        assertTrue(result.isPresent());
        assertEquals(expectedClient, result.get());

        verify(clientRepo, times(1)).getClientByChatId(chatId);
    }

    @Test
    void getByChatId_NonExistingChatId_ReturnsEmptyOptional() {

        long chatId = 456;
        when(clientRepo.getClientByChatId(chatId)).thenReturn(Optional.empty());


        Optional<Client> result = clientService.getByChatId(chatId);


        assertTrue(result.isEmpty());


        verify(clientRepo, times(1)).getClientByChatId(chatId);
    }

    @Test
    void create_Client_ReturnsSavedClient() {
        // Arrange
        Client clientToSave = new Client();
        Client savedClient = new Client();
        when(clientRepo.save(clientToSave)).thenReturn(savedClient);

        Client result = clientService.create(clientToSave);

        assertEquals(savedClient, result);

        verify(clientRepo, times(1)).save(clientToSave);
    }

    @Test
    void delete_ExistingId_DeletesClient() {
        long clientId = 789;

        clientService.delete(clientId);

        verify(clientRepo, times(1)).deleteById(clientId);
    }
}
