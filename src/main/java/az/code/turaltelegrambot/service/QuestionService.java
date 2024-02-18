package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.entity.Question;
import az.code.turaltelegrambot.repository.QuestionRepository;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {
    private final QuestionRepository questionRepository;

    QuestionService(QuestionRepository questionRepository){
        this.questionRepository=questionRepository;
    }

    public Question findById(Long i){
        return questionRepository.findById(i).get();
    }


}
