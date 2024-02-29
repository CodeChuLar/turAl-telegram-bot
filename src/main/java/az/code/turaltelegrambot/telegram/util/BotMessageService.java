package az.code.turaltelegrambot.telegram.util;

import az.code.turaltelegrambot.entity.Language;

import java.util.Optional;

public interface BotMessageService {
    Optional<BotMessage> getBy(String key, Language language);
}
