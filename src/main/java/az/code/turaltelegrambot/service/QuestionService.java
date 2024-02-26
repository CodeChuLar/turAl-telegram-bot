package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Question;

import java.util.Optional;

public interface QuestionService {
    Optional<Question> findById(Long i);

    Optional<Question> findByKey(String key);
}
