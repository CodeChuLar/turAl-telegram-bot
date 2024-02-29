package az.code.turaltelegrambot.telegram;

import az.code.turaltelegrambot.entity.*;
import az.code.turaltelegrambot.redis.RedisEntity;
import az.code.turaltelegrambot.redis.RedisService;
import az.code.turaltelegrambot.service.*;
import az.code.turaltelegrambot.telegram.util.CurrentQuestion;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
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

    private final RedisService redisService;
    private final RedisEntity redisEntity = new RedisEntity();

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Long, Language> chatLanguage = new HashMap<>();
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");


    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            Optional<RedisEntity> redisFindByChatId = redisService.findByChatId(chatId);

            if (message.hasText() && redisFindByChatId.isEmpty()
                    && message.getText().equalsIgnoreCase("/start")) {
                redisEntity.setChatId(chatId);
                redisEntity.setActive(true);
                redisService.save(redisEntity);

                if (clientService.getByChatId(chatId).isEmpty()) {
                    sendPhoneRequest(chatId);
                } else if (!chatLanguage.containsKey(chatId))
                    sendFirstQuestion(update);

            } else if (message.hasText() && message.getText().equalsIgnoreCase("/stop")) {
                handleStopRequest(chatId);

            } else if (redisFindByChatId.isPresent() && message.hasContact()) {
                removeButtons(chatId);

                if (clientService.getByChatId(chatId).isEmpty()) {
                    handleNewClient(message);
                }

                sendFirstQuestion(update);
            } else {
                if (redisFindByChatId.isPresent() && checkFreetextAnswer(message)) {
                    sendQuestionAfterAnswer(update);
                } else {
                    try {
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("Please enter a valid option")
                                .build());
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                }
            }

        } else if (update.hasCallbackQuery()) {
            sendNextQuestionByOption(update);
        }
        return null;
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

            CurrentQuestion.setMessage(chatId, localizationService.translate(key, Language.AZ));
        }
    }

    private void sendNextQuestionByOption(Update update) {
        if (update.hasCallbackQuery()) {
            MaybeInaccessibleMessage maybeMessage = update.getCallbackQuery().getMessage();

            Long chatId = maybeMessage.getChatId();
            Option chosenOption = getChosenOption(update.getCallbackQuery().getData(), chatId);

            if (!chatLanguage.containsKey(chatId))
                setLanguage(chatId, update.getCallbackQuery().getData());

            Optional<Question> nextQuestion = questionService.findById(Objects
                    .requireNonNull(chosenOption).getNextQuestionId());
            if (nextQuestion.isPresent()) {
                String translatedQuestion = localizationService.translate(nextQuestion.get().getKey(),
                        chatLanguage.get(chatId));
                List<String> translatedOptions = optionService.findByQuestionId(nextQuestion
                                .get().getId()).stream()
                        .map(option -> localizationService.translate(option.getKey(),
                                chatLanguage.get(chatId)))
                        .toList();

                sendQuestion(chatId, translatedQuestion, translatedOptions);

                if (CurrentQuestion.getMessage() != null) {
                    String translatedChosenOption = localizationService.translate(chosenOption.getKey(), chatLanguage.get(chatId));
                    redisEntity.setCurrentQuestion(nextQuestion.get().getKey());
                    redisEntity.setLanguage(chatLanguage.get(chatId));
                    redisEntity.getAnswers().put(localizationService.findByValue(CurrentQuestion.getMessage().getText()), translatedChosenOption);
                    redisService.save(redisEntity);
                }

                CurrentQuestion.setMessage(chatId, translatedQuestion);
            } else if (clientService.getByChatId(chatId).isPresent()) {
                handleNewSession(chatId);
            }
        }
    }

    private Option getChosenOption(String answer, long chatId) {
        String optionKey = localizationService.findByValue(answer);
        Optional<Option> chosenOption = optionService.findByKey(optionKey);

        if (chosenOption.isPresent()) {
            String formattedChosenOption = "*" + answer + "*";

            try {
                execute(SendMessage.builder().chatId(chatId).text("You chose: " + formattedChosenOption).parseMode(ParseMode.MARKDOWN).build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }

            return chosenOption.get();
        } else {
            try {
                execute(SendMessage.builder().chatId(chatId).text("Please choose one").build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
            return null;
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

    public boolean checkFreetextAnswer(Message message) {
        if (CurrentQuestion.getMessage() == null)
            return false;

        Optional<Question> previousQuestionAnswer = questionService.findByKey(
                localizationService.findByValue(
                        CurrentQuestion.getMessage().getText()));

        if (message.hasText() && previousQuestionAnswer.isPresent()) {

            if (previousQuestionAnswer.get().getOptionList().get(0).getKey().equals("dateRange")) {
                String regexPattern = "\\d{2}\\.\\d{2}\\.\\d{4},\\d{2}\\.\\d{2}\\.\\d{4}";
                Pattern pattern = Pattern.compile(regexPattern);
                Matcher matcher = pattern.matcher(message.getText());

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
                        log.error("Can't parse entered date: " + e.getMessage());
                        return false;
                    }

                    LocalDate today = LocalDate.now();
                    return firstLocalDate.isAfter(today.plusDays(1))
                            && firstLocalDate.isBefore(secondLocalDate);
                }
                return false;
            } else if (previousQuestionAnswer.get()
                    .getOptionList().get(0).getKey().equals("budget")
                    || previousQuestionAnswer.get()
                    .getOptionList().get(0).getKey().equals("count")
            ) {
                try {
                    Long.parseLong(message.getText());
                    return true;
                } catch (NumberFormatException e) {
                    log.error("Cant parse answer to long: " + e.getMessage());
                    return false;
                }
            } else return previousQuestionAnswer.get()
                    .getOptionList().get(0).getKey().equals("freetext");
        }
        return false;
    }

    private void sendQuestionAfterAnswer(Update update) {
        Message message = update.getMessage();
        Optional<Question> previousQuestionAnswer = questionService.findByKey(
                localizationService.findByValue(CurrentQuestion.getMessage().getText()));

        if (previousQuestionAnswer.isPresent()
                && !previousQuestionAnswer.get().getOptionList().isEmpty()//TODO
                && previousQuestionAnswer.get().getOptionList().size() == 1) {
            Option nullOption = previousQuestionAnswer.get().getOptionList().get(0);
            Optional<Question> nextQuestion = questionService.findById(nullOption.getNextQuestionId());
            if (nextQuestion.isPresent()) {
                if (CurrentQuestion.getMessage() != null) {
                    redisEntity.setCurrentQuestion(nextQuestion.get().getKey());
                    redisEntity.setLanguage(chatLanguage.get(message.getChatId()));
                    redisEntity.getAnswers().put(localizationService.findByValue(CurrentQuestion.getMessage().getText()), message.getText());
                    redisService.save(redisEntity);
                }

                String translatedQuestion = localizationService.translate(nextQuestion.get().getKey(), chatLanguage.get(message.getChatId()));
                List<String> translatedOptions = optionService.findByQuestionId(nextQuestion.get().getId()).stream()
                        .map(option -> localizationService.translate(option.getKey(), chatLanguage.get(message.getChatId())))
                        .toList();

                sendQuestion(message.getChatId(), translatedQuestion, translatedOptions);
                if (CurrentQuestion.getMessage() != null)
                    CurrentQuestion.setMessage(message.getChatId(), translatedQuestion);
            } else {
                handleNewSession(message.getChatId());
            }
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

    private void handleNewSession(Long chatId) {
        Optional<Client> client = clientService.getByChatId(chatId);
        if (client.isPresent()) {
            JSONObject object = new JSONObject();

            redisEntity.getAnswers().forEach(object::put);

            Session session = Session.builder()
                    .id(UUID.randomUUID())
                    .client(client.get())
                    .answers(object.toString())
                    .active(true)
                    .registeredAt(LocalDateTime.now())
                    .build();
            sessionService.create(session);
            System.out.println("Session with id: " + session.getId() + " is now active");

            // Instead of sending just the JSON object, send the session object
            sendSessionToAnotherApp(session);

            redisService.clearCache();

            sendWaitingMessageToClient(chatId, chatLanguage.get(chatId));
        }
    }

    private void sendSessionToAnotherApp(Session session) {
        kafkaTemplate.send(new ProducerRecord<>("session-new-topic", session));
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

        sendClientToAnotherApp(clientCreated);

        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("Thank you for trusting us %s !", clientCreated.getFullName()))
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void sendClientToAnotherApp(Client client) {
        kafkaTemplate.send(new ProducerRecord<>("client-new-topic", client));
    }

    private void handleStopRequest(long chatId) {
        Optional<RedisEntity> redisEntity = redisService.findByChatId(chatId);
        if (redisEntity.isPresent()) {
            redisService.remove(chatId);
            redisService.clearCache();
            chatLanguage.remove(chatId);
            Optional<Session> stoppingSession = sessionService.getByChatId(chatId);
            stoppingSession.ifPresent(session -> sessionService.delete(session.getId()));
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
        execute(SetMyCommands.builder().commands(List.of(
                        new BotCommand("start", "Start bot"),
                        new BotCommand("stop", "Stop connection")))
                .build());
    }
}
