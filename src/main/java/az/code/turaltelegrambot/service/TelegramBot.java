package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.config.BotConfig;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private static int option1SelectedCount = 0;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String text = message.getText();
            if ("/start".equals(text)) {
                List<String> options = new ArrayList<>();
                options.add("Istirahet");
                options.add("Gezinti");

                sendQuestionWithButtons(chatId, "Konlunden nece bir seyahet kecir?", options);

            }
        } else if (update.getCallbackQuery().getData().equals("Istirahet") || update.getCallbackQuery().getData().equals("Gezinti")) {
            System.out.println(update.getCallbackQuery().getData());
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();
            long chatId = callbackQuery.getMessage().getChatId();
            List<String> options = new ArrayList<>();
            options.add("Hersey daxil");
            options.add("Option 2");
            sendQuestionWithButtons(chatId, "Nece bir teklif seni maraqlandirir?", options);
        } else if (update.getCallbackQuery().getData().equals("Hersey daxil")) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
//            String callbackData = callbackQuery.getData();
            long chatId = callbackQuery.getMessage().getChatId();
            List<String> options = new ArrayList<>();
            options.add("Olkedaxili");
            options.add("Olkexarici");
            sendQuestionWithButtons(chatId, "Olkedaxili yoxsa Olkexarici?", options);
        } else if (update.getCallbackQuery().getData().equals("Olkexarici") || update.getCallbackQuery().getData().equals("Olkedaxili")) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
//            String callbackData = callbackQuery.getData();
            long chatId = callbackQuery.getMessage().getChatId();
            List<String> options = new ArrayList<>();
            options.add("Boyuk qrup ile");
            options.add("Kicik qrup ile");
            sendQuestionWithButtons(update.getMessage().getChatId(), "Seyahet tipi ?", options);
        }
    }

    private void sendQuestionWithButtons(long chatId, String question, List<String> options) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(question);

        // Create inline keyboard
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String option : options) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(option);
            inlineKeyboardButton.setCallbackData(option);
            rowInline.add(inlineKeyboardButton);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(markupInline);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }


}