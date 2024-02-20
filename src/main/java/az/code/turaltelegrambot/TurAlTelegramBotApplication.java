package az.code.turaltelegrambot;

import az.code.turaltelegrambot.telegram.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@Slf4j
public class TurAlTelegramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurAlTelegramBotApplication.class, args);
    }

}
