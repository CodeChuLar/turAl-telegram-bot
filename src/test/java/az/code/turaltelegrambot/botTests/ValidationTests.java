package az.code.turaltelegrambot.botTests;

import az.code.turaltelegrambot.telegram.TelegramBot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.Message;

public class ValidationTests {
    TelegramBot telegrambot;
    @BeforeEach
    void init(){
        telegrambot = new TelegramBot(
                Mockito.mock(),
                Mockito.mock(),
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void answerValidation_true() {
        Assertions.assertTrue(telegrambot.checkFreetextAnswer(new Message()));
    }
    @Test
    void answerValidation_false() {
        Assertions.assertFalse(telegrambot.checkFreetextAnswer(new Message()));
    }

}
