package az.code.turaltelegrambot.botTests;


import az.code.turaltelegrambot.entity.Language;
import az.code.turaltelegrambot.telegram.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class WaitingForOffersTests {

    @Mock
    private TelegramApiException telegramApiException;

    @Mock
    private TelegramBot telegramBot;

    @Captor
    private ArgumentCaptor captor;

    @BeforeEach
    void setUp() {
        try(AutoCloseable autoCloseable = MockitoAnnotations.openMocks(this)){
            System.out.printf(autoCloseable.toString());
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Test
    void sendWaitingMessageToClient_Success() throws TelegramApiException {
        long chatId = 123456789L;
        Language language = Language.EN;
        String expectedMessage = "Your request has been recorded. Offers will be sent to you as soon as possible.";
        doNothing().when(telegramBot).execute(any(SendMessage.class));
//        verify(SendMessage.class).
//        Assertions.assertAll(() -> telegramBot.sendWaitingMessageToClient(chatId, language));

    }

    @Test
    void sendWaitingMessageToClient_Exception() throws TelegramApiException {
        long chatId = 123456789L;
        Language language = Language.EN;

        doThrow(telegramApiException).when(telegramBot).execute(any(SendMessage.class));

        telegramBot.sendWaitingMessageToClient(chatId, language);

        verify(telegramBot, times(1)).execute(any(SendMessage.class));
    }
}


