package az.code.turaltelegrambot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class BotConfig {

    @Value("${bot.username}")
    String username;
    @Value("${bot.token}")
    String token;

}
