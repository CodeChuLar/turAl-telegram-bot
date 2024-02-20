package az.code.turaltelegrambot.repository;

import az.code.turaltelegrambot.entity.Language;
import az.code.turaltelegrambot.entity.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocaleRepository extends JpaRepository<Locale, Long> {
    Optional<Locale> findByKeyAndLanguage(String key, Language language);
    Optional<Locale> findByValue(String value);
}