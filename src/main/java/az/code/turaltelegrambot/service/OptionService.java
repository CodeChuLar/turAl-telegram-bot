package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Option;

import java.util.List;
import java.util.Optional;

public interface OptionService {
    List<Option> findByQuestionId(Long questionId);

    Optional<Option> findByKey(String key);
}
