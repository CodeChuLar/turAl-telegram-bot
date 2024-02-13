package az.code.turaltelegrambot.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    List<Answer> answersList;

    @ManyToOne
    Client client;
    boolean isDone;
    LocalDateTime registeredAt;
}
