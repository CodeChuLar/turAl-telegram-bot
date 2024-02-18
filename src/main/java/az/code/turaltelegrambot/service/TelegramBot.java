package az.code.turaltelegrambot.service;

import az.code.turaltelegrambot.config.BotConfig;
import az.code.turaltelegrambot.entity.Client;
import az.code.turaltelegrambot.entity.Language;
import az.code.turaltelegrambot.entity.Option;
import az.code.turaltelegrambot.entity.Question;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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

    private QuestionService questionService;
    private Long nextQuestionId;

    private LocalizationService localizationService;

    private OptionService optionService;

    public TelegramBot(BotConfig botConfig, ClientService clientService, QuestionService questionService, LocalizationService localizationService, OptionService optionService) {
        this.botConfig = botConfig;
        this.clientService = clientService;
        this.questionService = questionService;
        this.localizationService = localizationService;
        this.optionService = optionService;
    }

    private final Set<Long> startedConversations = new HashSet<>();
    private Map<Long, Language> chatLanguage = new HashMap<>();



    @Override
    public void onUpdateReceived(Update update) {
        List<String> answers = new ArrayList<>();

        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            if (message.hasText() && !startedConversations.contains(chatId)
                    && message.getText().equalsIgnoreCase("/start")) {
                System.out.println("11111111111");
                startedConversations.add(chatId);
                if (clientService.getByChatId(chatId).isEmpty())
                    sendPhoneRequest(chatId);
            } else if (startedConversations.contains(chatId) && message.hasContact()) {
                System.out.println("22222222222222");
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
            }
//            else if (startedConversations.contains(chatId)) {
//                try {
//                    execute(SendMessage.builder().chatId(chatId).text("Please, enter valid answer").build());
//                } catch (TelegramApiException e) {
//                    throw new RuntimeException(e);
//                }
//            }
        } else if (update.hasCallbackQuery() || update.hasMessage()
                && startedConversations.contains(update.getCallbackQuery()
                .getMessage().getChatId()) || startedConversations.contains(update
                .getMessage().getChatId())) {
            System.out.println("21wwww");
            sendNextQuestion(update);
        }
    }

    private void sendNextQuestion(Update update) {

        if (update.hasMessage() && nextQuestionId == null) {
            System.out.println("d[sadsdsap[ds[das");
            Message message = update.getMessage();
            long chatId = message.getChatId();

            Question theFirstQuestion = questionService.findById(1L);
            String key = theFirstQuestion.getKey();
            List<Option> optionList = theFirstQuestion.getOptionList();
            nextQuestionId = optionList.get(0).getNextQuestionId();

            sendQuestionWithButtons(chatId, key, optionList.stream().map(option -> option.getKey()).toList());

        } else if (update.hasCallbackQuery() || update.hasMessage()) {
            //        Option option = validateOption(update.getCallbackQuery().getMessage().getText());

            if (!chatLanguage.containsKey(update.getCallbackQuery().getMessage().getChatId())) {
                chatLanguage.put(update.getCallbackQuery().getMessage().getChatId(),
                        Language.getByText(update.getCallbackQuery().getData()));
                System.out.println("sadasdsadadadasdasd");
            } else {
                if (update.hasCallbackQuery()) {
                    String byLanguageAndValue = localizationService.findByLanguageAndValue(chatLanguage.get(update.getCallbackQuery().getMessage().getChatId()), update.getCallbackQuery().getData());
                    Option optionByKey = optionService.findByKeyAndQuestionId(byLanguageAndValue, nextQuestionId);
                    nextQuestionId = optionByKey.getNextQuestionId();
                } else if (update.hasMessage()){
//                    nextQuestionId= 9L;
                    System.out.println("dasdads");
                    nextQuestionId= questionService.findById(nextQuestionId).getOptionList().get(0).getNextQuestionId();
                }
            }
        }

        Question byId = questionService.findById(nextQuestionId);
//            System.out.println(byId.getOptionList().get(0));

        System.out.println(byId);
//        String translatedQuestion = localizationService.translate(byId.getKey(),
//                chatLanguage.get(update.getCallbackQuery().getMessage().getChatId()));
        if (update.hasMessage()) {
            String translatedQuestion = localizationService.translate(byId.getKey(),
                    chatLanguage.get(update.getMessage().getChatId()));
            sendQuestion(update.getMessage().getChatId(), translatedQuestion);
        } else {
            String translatedQuestion = localizationService.translate(byId.getKey(),
                    chatLanguage.get(update.getCallbackQuery().getMessage().getChatId()));
            if (byId.getOptionList().get(0).getKey() == null){
                sendQuestion(update.getCallbackQuery().getMessage().getChatId(), translatedQuestion);
            }
            else {
                List<String> translatedOptions = byId.getOptionList().stream().map(option -> localizationService.translate(
                        option.getKey(), chatLanguage.get(update.getCallbackQuery().getMessage().getChatId()))).toList();
                sendQuestionWithButtons(update.getCallbackQuery().getMessage().getChatId(), translatedQuestion, translatedOptions);


            }
        }
    }



    public void sendQuestion(long chatId, String question) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(question);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendQuestionWithButtons(long chatId, String question, List<String> optionsKeys) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(question);

        // Create inline keyboard
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String optionKey : optionsKeys) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(optionKey);
            inlineKeyboardButton.setCallbackData(optionKey);
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