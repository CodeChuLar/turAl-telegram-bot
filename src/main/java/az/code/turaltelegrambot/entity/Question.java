package az.code.turaltelegrambot.entity;

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
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    String key;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "question")
    List<Option> optionList;
}
