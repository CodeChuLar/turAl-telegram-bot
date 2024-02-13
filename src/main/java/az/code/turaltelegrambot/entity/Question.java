package az.code.turaltelegrambot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<QuestionLocale> localeList;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<Answer> answer;
}
