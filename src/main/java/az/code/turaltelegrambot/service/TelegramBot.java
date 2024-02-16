package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.config.BotConfig;
import az.code.turaltelegrambot.entity.Client;
import az.code.turaltelegrambot.entity.Language;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Service
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private static int option1SelectedCount = 0;

    private final ClientService clientService;

    public TelegramBot(BotConfig botConfig, ClientService clientService) {
        this.botConfig = botConfig;
        this.clientService = clientService;
    }

    private final Set<Long> startedConversations = new HashSet<>();
    private final Map<Long, Language> chatLanguage = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        List<String> answers = new ArrayList<>();

        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            if (message.hasText() && !startedConversations.contains(chatId)
                    && message.getText().equalsIgnoreCase("/start")) {
                startedConversations.add(chatId);
                if (clientService.getByChatId(chatId).isEmpty())
                    sendPhoneRequest(chatId);
            } else if (startedConversations.contains(chatId) && message.hasContact()) {
                removeButtons(chatId);
                if (clientService.getByChatId(chatId).isEmpty()) {
                    Contact contact = message.getContact();
                    String firstName = contact.getFirstName() != null ? contact.getFirstName() : "";
                    String lastName = contact.getLastName() != null ? contact.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();

                    Client clientCreated = Client.builder()
                            .clientId(chatId)
                            .chatId(chatId)
                            .fullName(fullName)
                            .phoneNumber(contact.getPhoneNumber())
                            .build();
                    clientService.create(clientCreated);

                    try {
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text(String.format("Thank you for trusting us %s !", clientCreated.getFullName()))
                                .build());
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                }
                sendNextQuestion(update);

            } else if (message.hasText() && message.getText().equalsIgnoreCase("/stop")) {
                if (startedConversations.contains(chatId)) {
                    startedConversations.remove(chatId);
                    if (clientService.getByChatId(chatId).isPresent()) {
                        clientService.delete(clientService.getByChatId(chatId).get().getClientId());
                    }
                    try {
                        removeButtons(chatId);
                        execute(SendMessage.builder().chatId(chatId).text("Chat stopped.").build());
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                } else {
                    try {
                        execute(SendMessage.builder().chatId(chatId).text("Enter /start to begin").build());
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                }
            } else if (startedConversations.contains(chatId)) {
                try {
                    execute(SendMessage.builder().chatId(chatId).text("Please, enter valid answer").build());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (update.hasCallbackQuery()
                && startedConversations.contains(update.getCallbackQuery()
                .getMessage().getChatId())) {
            sendNextQuestion(update);
        }
    }

    private void sendNextQuestion(Update update) {
//        Option option = validateOption(update.getCallbackQuery().getMessage().getText());
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

        /*long chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            String answer = update.getCallbackQuery().getData();
            System.out.println("Answer: " + answer);
        } else {
            chatId = update.getMessage().getChatId();
            Language language;
            if (update.getMessage().hasText())
                language = Language.getByText(update.getMessage().getText());
//                if (language != null) {
//                    chatLanguage = language;
//                } else {
//                    try {
//                        execute(SendMessage.builder().chatId(chatId).text("Please choose from available options").build());
//                    } catch (TelegramApiException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
            System.out.println("Message: " + update.getMessage().getText());
        }

        String questionText = "What is your favorite color?";
        List<String> options = new ArrayList<>();
        options.add("Red");
        options.add("Blue");
        options.add("Green");

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(questionText);

        InlineKeyboardMarkup markupInline = getInlineKeyboardMarkup(options);

        sendMessage.setReplyMarkup(markupInline);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }*/


    @NotNull
    private static InlineKeyboardMarkup getInlineKeyboardMarkup(List<String> options) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String option : options) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(option);
            button.setCallbackData(option); // You can set callback data here if needed
            rowInline.add(button);
            rowsInline.add(rowInline);
        }
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    private void sendPhoneRequest(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Please share your phone number with us:");

        ReplyKeyboardMarkup keyboardMarkup = getReplyKeyboardMarkup();

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @NotNull
    private static ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardRow1 = new KeyboardRow();

        KeyboardButton shareContactButton = new KeyboardButton();
        shareContactButton.setText("Share Contact");
        shareContactButton.setRequestContact(true);

        keyboardRow1.add(shareContactButton);
        keyboard.add(keyboardRow1);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private void removeButtons(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);
        replyKeyboardRemove.setSelective(true);
        sendMessage.setReplyMarkup(replyKeyboardRemove);
        sendMessage.setText("Okay");

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
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