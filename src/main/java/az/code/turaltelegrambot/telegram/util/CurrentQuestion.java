package az.code.turaltelegrambot.telegram.util;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;

public class CurrentQuestion {
    @Setter
    @Getter
    private static Message message;

    public static void setMessage(long chatId, String translatedQuestion) {
        Message message1 = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        message1.setChat(chat);
        message1.setText(translatedQuestion);
        CurrentQuestion.message = message1;
    }
}
