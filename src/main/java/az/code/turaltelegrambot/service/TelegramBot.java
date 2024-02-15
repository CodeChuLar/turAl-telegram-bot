package az.code.turaltelegrambot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;


@Service
public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String text = message.getText();

            if ("/start".equals(text)) {
                sendPhoneRequest(chatId);
            } else if (message.getContact() != null && message.getContact().getPhoneNumber() != null) {
                Contact contact = message.getContact();
                System.out.println("Received contact: " + contact.getPhoneNumber());
                removeButtons(chatId);
            } else {
                sendPhoneRequest(chatId);
            }
        }
    }

    private void sendPhoneRequest(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Please share your phone number with us:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardRow1 = new KeyboardRow();
        KeyboardRow keyboardRow2 = new KeyboardRow();

        KeyboardButton shareContactButton = new KeyboardButton();
        shareContactButton.setText("Share Contact");
        shareContactButton.setRequestContact(true);

        keyboardRow1.add(shareContactButton);
        keyboard.add(keyboardRow1);

        keyboardMarkup.setKeyboard(keyboard);

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void removeButtons(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Thank you for sharing your contact.");

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true); // Set removeKeyboard to true

        sendMessage.setReplyMarkup(replyKeyboardRemove);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "TURAL_AL_bOt";
    }

    @Override
    public String getBotToken() {
        return "6272012662:AAEmo1Hsd5Th-r35YjBOlD7aOwUhSUtROhg";
    }

}
