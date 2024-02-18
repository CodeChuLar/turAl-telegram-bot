package az.code.turaltelegrambot.repository;


import az.code.turaltelegrambot.entity.Option;
import az.code.turaltelegrambot.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OptionRepository  extends JpaRepository<Option, Long> {

    Option findFirstByKey(String key);
//    Optional<Option> findByKey(String key);

    Optional<Option> findByKeyAndQuestion_Id(String key, Long questionId);

}
