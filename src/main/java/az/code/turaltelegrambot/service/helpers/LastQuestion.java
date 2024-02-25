package az.code.turaltelegrambot.service.helpers;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Message;

public class LastQuestion {
    @Getter
    private static long lastBotMessageChatId;
    @Getter
    private static Message lastBotMessage;

    public static void setLastBotMessage(Message message) {
        lastBotMessage = message;
        lastBotMessageChatId = message.getChatId();
    }
}
