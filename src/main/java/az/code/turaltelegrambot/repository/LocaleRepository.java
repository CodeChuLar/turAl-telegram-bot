package az.code.turaltelegrambot.repository;

import az.code.turaltelegrambot.entity.Language;
import az.code.turaltelegrambot.entity.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocaleRepository extends JpaRepository<Locale, Long> {
    Locale findByKeyAndLanguage(String key, Language language);
    Locale findByLanguageAndValue(Language language, String value);

}