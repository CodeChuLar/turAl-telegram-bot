package az.code.turaltelegrambot;

import az.code.turaltelegrambot.entity.Language;
import az.code.turaltelegrambot.entity.Locale;
import az.code.turaltelegrambot.entity.Option;
import az.code.turaltelegrambot.entity.Question;
import az.code.turaltelegrambot.repository.LocaleRepository;
import az.code.turaltelegrambot.repository.OptionRepository;
import az.code.turaltelegrambot.repository.QuestionRepository;
import az.code.turaltelegrambot.service.LocalizationService;
import az.code.turaltelegrambot.service.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SpringBootApplication
@Slf4j
public class TurAlTelegramBotApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(TurAlTelegramBotApplication.class, args);
        TelegramBot telegramBot = context.getBean(TelegramBot.class);
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }

    }

}
