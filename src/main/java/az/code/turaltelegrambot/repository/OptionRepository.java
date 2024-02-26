package az.code.turaltelegrambot.repository;


import az.code.turaltelegrambot.entity.Option;
import az.code.turaltelegrambot.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OptionRepository  extends JpaRepository<Option, Long> {
    Optional<Option> findByKey(String key);
    List<Option> findByQuestion_Id(Long questionId);

}
