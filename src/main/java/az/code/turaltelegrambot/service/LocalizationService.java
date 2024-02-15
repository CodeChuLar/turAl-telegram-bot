package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Language;
import az.code.turaltelegrambot.repository.LocaleRepository;
import az.code.turaltelegrambot.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocalizationService {

    @Autowired
    private LocaleRepository localeRepository;



    public String translate(String key, Language language) {
        return localeRepository.findByKeyAndLanguage(key, language).getValue();
    }
}
