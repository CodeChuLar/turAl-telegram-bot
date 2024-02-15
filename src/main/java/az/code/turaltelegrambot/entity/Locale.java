package az.code.turaltelegrambot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Locale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    Language language;
    String key;
    String value;
}
