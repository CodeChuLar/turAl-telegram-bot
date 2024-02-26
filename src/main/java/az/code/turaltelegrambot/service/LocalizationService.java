package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Language;

public interface LocalizationService {
    String translate(String key, Language language);

    String findByValue(String value);
}
