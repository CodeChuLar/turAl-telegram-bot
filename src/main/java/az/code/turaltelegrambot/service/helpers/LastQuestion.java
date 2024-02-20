package az.code.turaltelegrambot.service.helpers;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Message;

public class LastQuestion {
    @Getter
    private static long lastBotMessageChatId; // Store the chat ID of the last bot message
    @Getter
    private static Message lastBotMessage; // Store the last bot message

    public static void setLastBotMessage(Message message) {
        lastBotMessage = message;
        lastBotMessageChatId = message.getChatId();
    }
}
