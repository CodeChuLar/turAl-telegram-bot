package az.code.turaltelegrambot.redis;

import az.code.turaltelegrambot.entity.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.HashMap;

@Data
@Builder()
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "redisEntity")
public class RedisEntity implements Serializable {

    @Id
    private Long chatId;
    private Language language;
    private String currentQuestion;
    private HashMap<String, String> answers = new HashMap<>();
    private boolean isActive;
}
