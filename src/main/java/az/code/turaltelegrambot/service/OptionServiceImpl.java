package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Option;
import az.code.turaltelegrambot.repository.OptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OptionServiceImpl implements OptionService {
    private final OptionRepository optionRepository;

    public OptionServiceImpl(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public List<Option> findByQuestionId(Long questionId) {
        return optionRepository.findByQuestion_Id(questionId);
    }

    public Optional<Option> findByKey(String key) {
        return optionRepository.findByKey(key);
    }

}
