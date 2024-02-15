package az.code.turaltelegrambot;

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

//package com.example.telegramturappmain;

//package com.example.telegramturappmain;


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
                System.out.println(message.getContact().getPhoneNumber());
            } else if (message.getContact().getPhoneNumber() != null) {
                // User has shared contact, remove buttons
                Contact contact = message.getContact();
                System.out.println("Received contact: " + contact.getPhoneNumber());
                removeButtons(chatId);
            } else {
                // User has not shared contact, prompt again
                sendPhoneRequest(chatId);
            }
        }
    }

    private void sendPhoneRequest(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Please share your phone number with us:");

        // Create keyboard markup with buttons for sharing contact
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        // Create two keyboard rows
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardRow1 = new KeyboardRow();
        KeyboardRow keyboardRow2 = new KeyboardRow();

        // Add buttons for sharing contact
        KeyboardButton shareContactButton = new KeyboardButton();
        shareContactButton.setText("Share Contact");
        shareContactButton.setRequestContact(true);

        // Add buttons to rows
        keyboardRow1.add(shareContactButton);
        keyboard.add(keyboardRow1);

        // Set keyboard to markup
        keyboardMarkup.setKeyboard(keyboard);

        // Add markup to the message
        sendMessage.setReplyMarkup(keyboardMarkup);

        // Send the message
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

        // Remove keyboard markup
        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true); // Set removeKeyboard to true

        sendMessage.setReplyMarkup(replyKeyboardRemove);

        // Send the message
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "turAI_telegram_bot";
    }

    @Override
    public String getBotToken() {
        return "6962442608:AAFlckGLwTpYnoC0UVSYZQMwxIz8--wl-Dc";
    }

}
