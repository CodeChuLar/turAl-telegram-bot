package az.code.turaltelegrambot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionLocale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    Question question;

    @Enumerated(EnumType.STRING)
    Languages language;

    String translation;

    @OneToMany(mappedBy = "questionLocale")
    List<Option> options;
}
