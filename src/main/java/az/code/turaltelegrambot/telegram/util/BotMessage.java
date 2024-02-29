package az.code.turaltelegrambot.telegram.util;

import az.code.turaltelegrambot.entity.Language;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "botmessages")
public class BotMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    String key;
    @Enumerated(EnumType.STRING)
    Language language;
    String message;
}
