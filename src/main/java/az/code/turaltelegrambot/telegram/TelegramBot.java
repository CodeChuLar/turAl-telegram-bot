package az.code.turaltelegrambot.telegram;

import az.code.turaltelegrambot.entity.*;
import az.code.turaltelegrambot.service.*;
import az.code.turaltelegrambot.telegram.util.LastQuestion;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.SystemColor.text;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramWebhookBot {
    @Value("${bot.token}")
    private String secretApiKey;
    @Value("${bot.username}")
    private String botUsername;
    @Value("${bot.path}")
    private String webhookPath;

    private final ClientService clientService;
    private final QuestionService questionService;
    private final LocalizationService localizationService;
    private final OptionService optionService;
    private final SessionService sessionService;

    Set<Long> startedConversations = new HashSet<>(); //TODO startedconversations-u redisde saxlamaq lazimdir ki serveri restart edsek itmesin
    private final Map<Long, Language> chatLanguage = new HashMap<>();
    static final String DATE_PATTERN = "dd.MM.yyyy";
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);


    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            if (message.hasText() && !startedConversations.contains(chatId)
                    && message.getText().equalsIgnoreCase("/start")) {
                startedConversations.add(chatId);
                if (clientService.getByChatId(chatId).isEmpty())
                    sendPhoneRequest(chatId);
            } else if (message.hasText() && message.getText().equalsIgnoreCase("/stop")) {
                handleStopRequest(chatId);
            } else if (startedConversations.contains(chatId) && message.hasContact()) {
                removeButtons(chatId);
                if (clientService.getByChatId(chatId).isEmpty()) {
                    handleNewClient(message);
                }
                sendFirstQuestion(update);
            } else {
                if (startedConversations.contains(message.getChatId()) && checkMessage(message))
                    sendQuestionAfterAnswer(update);
                else {
                    try {
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("Please enter valid option")
                                .build());
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } else if (update.hasCallbackQuery()
                && startedConversations.contains(update.getCallbackQuery().getMessage().getChatId())) {
            sendNextQuestionByOption(update);
        }
        return null;
    }

    private boolean checkMessage(Message message) {
        boolean valid = false;
        Optional<Question> previousQuestionWithNoOptions = questionService.findByKey(localizationService.findByValue(LastQuestion.getLastBotMessage().getText()));
        if (message.hasText() && previousQuestionWithNoOptions.isPresent()) {
            if (previousQuestionWithNoOptions.get().getOptionList().get(0).getKey().equals("dateRange")) {
                String regexPattern = "\\d{2}\\.\\d{2}\\.\\d{4},\\d{2}\\.\\d{2}\\.\\d{4}";
                Pattern pattern = Pattern.compile(regexPattern);
                Matcher matcher = pattern.matcher(message.getText());
                System.out.println(message.getText());
                if (matcher.matches()) {
                    String matchedDates = matcher.group();
                    String[] dates = matchedDates.split(",");
                    String firstDate = dates[0];
                    String secondDate = dates[1];
                    LocalDate firstLocalDate;
                    LocalDate secondLocalDate;
                    try {
                        firstLocalDate = LocalDate.parse(firstDate, DATE_FORMATTER);
                        secondLocalDate = LocalDate.parse(secondDate, DATE_FORMATTER);
                    } catch (DateTimeParseException e) {
                        return false;
                    }

                    LocalDate today = LocalDate.now();
                    if (firstLocalDate.isAfter(today.plusDays(1)) && firstLocalDate.isBefore(secondLocalDate)) {
                        return true;
                    } else {
                        return false;
                    }
//                return matcher.matches();
                }
            }
            else if (previousQuestionWithNoOptions.get().getOptionList().get(0).getKey().equals("budget")) {
                try {
                    long l = Long.parseLong(message.getText());
                    System.out.println(l);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }

            }
            return true;
        }
        return  true;
    }

    private void sendQuestionAfterAnswer(Update update) {
        Message message = update.getMessage();
        Optional<Question> previousQuestionWithNullOption = questionService.findByKey(
                localizationService.findByValue(LastQuestion.getLastBotMessage().getText()));

//        System.out.println("size: " + previousQuestionWithNullOption.get().getOptionList().size());
        if (previousQuestionWithNullOption.isPresent()
                && !previousQuestionWithNullOption.get().getOptionList().isEmpty()
                && previousQuestionWithNullOption.get().getOptionList().size() == 1) {
            Option nullOption = previousQuestionWithNullOption.get().getOptionList().get(0);
            Optional<Question> nextQuestion = questionService.findById(nullOption.getNextQuestionId());
            if (nextQuestion.isPresent()) {
                String translatedQuestion = localizationService.translate(nextQuestion.get().getKey(), chatLanguage.get(message.getChatId()));
                List<String> translatedOptions = optionService.findByQuestionId(nextQuestion.get().getId()).stream()
                        .map(option -> localizationService.translate(option.getKey(), chatLanguage.get(message.getChatId())))
                        .toList();
                sendQuestion(message.getChatId(), translatedQuestion, translatedOptions);
                Message message1 = new Message();
                Chat chat = new Chat();
                chat.setId(message.getChatId());
                message1.setChat(chat);
                message1.setText(translatedQuestion);
                LastQuestion.setLastBotMessage(message1);
            } else handleNewSession(message.getChatId());
        }
    }

    private void sendFirstQuestion(Update update) {
        Message message = update.getMessage();
        long chatId = message.getChatId();
        Optional<Question> theFirstQuestion = questionService.findById(1L);
        if (theFirstQuestion.isPresent()) {
            String key = theFirstQuestion.get().getKey();
            List<Option> optionList = theFirstQuestion.get().getOptionList();
            sendQuestion(chatId,
                    localizationService.translate(key, Language.AZ),
                    optionList.stream().map(Option::getKey)
                            .toList());
            Message message1 = new Message();
            Chat chat = new Chat();
            chat.setId(chatId);
            message1.setChat(chat);
            message1.setText(localizationService.translate(key, Language.AZ));
            LastQuestion.setLastBotMessage(message);
        }
    }

    private void sendNextQuestionByOption(Update update) {
        if (update.hasCallbackQuery()) {
            try {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(update.getCallbackQuery()
                                .getId())
                        .build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
            MaybeInaccessibleMessage maybeMessage = update.getCallbackQuery().getMessage();

            if (maybeMessage instanceof Message) {
                Long chatId = maybeMessage.getChatId();
                Option chosenOption = getChosenOption(update.getCallbackQuery().getData(), chatId);

                if (!chatLanguage.containsKey(chatId))
                    setLanguage(chatId, update.getCallbackQuery().getData());
                //TODO delete/stop pressed buttons
//                if (LastQuestion.getLastBotMessage() != null) {
//                    Optional<Question> previousQuestion = questionService.findByKey(localizationService.findByValue(LastQuestion.getLastBotMessage().getText()));
//
//                    if (previousQuestion.isPresent()) {
//                        assert chosenOption != null;
//                        if (chosenOption.getNextQuestionId() > previousQuestion.get().getId()) {
//                            System.out.println(previousQuestion.get().getKey());
//                        }
//                    }
//                }

                Optional<Question> nextQuestion = questionService.findById(Objects.requireNonNull(chosenOption).getNextQuestionId());
                if (nextQuestion.isPresent()) {
                    String translatedQuestion = localizationService.translate(nextQuestion.get().getKey(), chatLanguage.get(chatId));
                    List<String> translatedOptions = optionService.findByQuestionId(nextQuestion.get().getId()).stream()
                            .map(option -> localizationService.translate(option.getKey(), chatLanguage.get(chatId)))
                            .toList();
                    sendQuestion(chatId, translatedQuestion, translatedOptions);
                    Message message = new Message();
                    Chat chat = new Chat();
                    chat.setId(chatId);
                    message.setChat(chat);
                    message.setText(translatedQuestion);
                    LastQuestion.setLastBotMessage(message);
                } else if (clientService.getByChatId(chatId).isPresent()) {
                    handleNewSession(chatId);
                }
            } else if (maybeMessage instanceof InaccessibleMessage inaccessibleMessage) {
                log.error("InaccessibleMessage: " + inaccessibleMessage);
                try {
                    execute(SendMessage.builder()
                            .chatId(inaccessibleMessage.getChatId())
                            .text("Something wen wrong. Try again later...")
                            .build());
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }
            }


        }
    }

    private void handleNewSession(Long chatId) {
        Optional<Client> client = clientService.getByChatId(chatId);
        if (client.isPresent()) {
            Session session = Session.builder()
                    .id(UUID.randomUUID())
                    .client(client.get())
                    .active(true)
                    .registeredAt(LocalDateTime.now())
                    .build();
            sessionService.create(session);
            System.out.println("Session with id: " + session.getId() + "is now active");

            //TODO connect session to redis

            sendWaitingMessageToClient(chatId, chatLanguage.get(chatId));
        }
    }

    private void sendWaitingMessageToClient(Long chatId, Language language) {
        String waitingMessage = getTranslatedWaitingMessage(language);
        assert waitingMessage != null;
        SendMessage sendMessage = SendMessage.builder().chatId(chatId).text(waitingMessage).build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private String getTranslatedWaitingMessage(Language language) {
        switch (language) {
            case AZ -> {
                return "Sorğunuz qeydə alındı. Ən qısa zamanda təkliflər sizə göndəriləcək.";
            }
            case EN -> {
                return "Your request has been recorded. Offers will be sent to you as soon as possible.";
            }
            case RU -> {
                return "Ваш запрос записан. Предложения будут отправлены вам как можно скорее.";
            }
            default -> {
                return null;
            }
        }
    }

    private void setLanguage(Long chatId, String data) {
        Language language = Language.getByText(data);
        if (language != null)
            chatLanguage.put(chatId, language);
        else {
            try {
                log.info("Client with chatId " + chatId + " chose unavailable language");
                execute(SendMessage.builder().chatId(chatId).text("Please choose one of available languages").build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    private Option getChosenOption(String answer, long chatId) {
        String optionKey = localizationService.findByValue(answer);
        if (optionService.findByKey(optionKey).isPresent())
            return optionService.findByKey(optionKey).get();

        try {
            execute(SendMessage.builder().chatId(chatId).text("Please choose one").build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private void handleStopRequest(long chatId) {
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

    private void handleNewClient(Message message) {
        long chatId = message.getChatId();
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

    private void sendQuestion(long chatId, String question, List<String> options) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(question);

        if (!options.isEmpty() && !options.contains(null)) {
            InlineKeyboardMarkup markupInline = getInlineKeyboardMarkup(options);
            sendMessage.setReplyMarkup(markupInline);
        }

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @NotNull
    private static InlineKeyboardMarkup getInlineKeyboardMarkup(List<String> options) {
        if (options.contains(null))
            return null;
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
    public String getBotPath() {
        return webhookPath;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return secretApiKey;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        execute(SetWebhook.builder().url(webhookPath).dropPendingUpdates(true).build());
        execute(SetMyCommands.builder().commands(List.of(new BotCommand("start", "Start bot"), new BotCommand("stop", "Deletes all your connection"))).build());
    }
}
