package az.code.turaltelegrambot.telegram.util;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;

public class LastQuestion {
    @Setter
    @Getter
    private static Message lastBotMessage;
}
