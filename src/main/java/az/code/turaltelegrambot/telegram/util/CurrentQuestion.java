package az.code.turaltelegrambot.telegram.util;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;

public class CurrentQuestion {
    @Setter
    @Getter
    private static Message message;
}
