package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Question;
import az.code.turaltelegrambot.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class QuestionServiceImpl implements QuestionService {
    private final QuestionRepository questionRepository;

    QuestionServiceImpl(QuestionRepository questionRepository){
        this.questionRepository=questionRepository;
    }

    public Optional<Question> findById(Long i){
        return questionRepository.findById(i);
    }

    public Optional<Question> findByKey(String key) {
        return questionRepository.findByKey(key);
    }
}
