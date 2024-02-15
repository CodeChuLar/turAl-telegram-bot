package az.code.turaltelegrambot.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "locales")
public class Locale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    Language language;
    String key;
    String value;
}
