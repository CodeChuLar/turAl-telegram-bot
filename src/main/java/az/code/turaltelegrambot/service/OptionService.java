package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Option;
import az.code.turaltelegrambot.repository.OptionRepository;
import org.springframework.stereotype.Service;

@Service
public class OptionService {

    private final OptionRepository optionRepository;

    public OptionService(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    public Option findByKeyAndQuestionId(String optionKey, Long questionId) {
        return optionRepository.findByKeyAndQuestion_Id(optionKey, questionId).get();
    }

//    public Option findByKey(String key) {
//        Option byKey = optionRepository.findByKey(key).get();
//        return byKey;
//    }

}
