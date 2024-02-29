package az.code.turaltelegrambot.telegram;

import az.code.turaltelegrambot.entity.*;
import az.code.turaltelegrambot.redis.RedisEntity;
import az.code.turaltelegrambot.redis.RedisService;
import az.code.turaltelegrambot.service.*;
import az.code.turaltelegrambot.telegram.util.BotMessage;
import az.code.turaltelegrambot.telegram.util.BotMessageService;
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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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

    private final BotMessageService botMessageService;

    private final RedisService redisService;

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
//                redisEntity.setChatId(chatId);
//                redisEntity.setActive(true);
                redisService.save(RedisEntity.builder()
                        .chatId(chatId)
                        .isActive(true)
                        .build());

                if (clientService.getByChatId(chatId).isEmpty()) {
                    sendPhoneRequest(chatId);
                } else if (!chatLanguage.containsKey(chatId))
                    sendFirstQuestion(update);

            } else if (message.hasText() && message.getText().equalsIgnoreCase("/stop")) {
                handleStopRequest(chatId);

            } else if (message.hasText() && message.getText().equalsIgnoreCase("/delete")) {
                handleDeleteRequest(chatId);

            } else if (redisFindByChatId.isPresent() && message.hasContact()) {
                removeButtons(chatId);

                if (clientService.getByChatId(chatId).isEmpty()) {
                    handleNewClient(message);
                }

                sendFirstQuestion(update);
            } else {
                if (redisFindByChatId.isPresent() && checkAnswer(message, redisFindByChatId.get())) {
                    sendQuestionAfterAnswer(chatId, message.getText());
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
        Optional<RedisEntity> currentRedis = redisService.findByChatId(chatId);
        if (theFirstQuestion.isPresent() && currentRedis.isPresent()) {
            String key = theFirstQuestion.get().getKey();
            List<Option> optionList = theFirstQuestion.get().getOptionList();

            sendQuestion(chatId,
                    localizationService.translate(key, Language.EN),
                    optionList.stream().map(Option::getKey)
                            .toList());
            currentRedis.get().setCurrentQuestionKey(theFirstQuestion.get().getKey());
            redisService.save(currentRedis.get());
        }
    }

    private void sendNextQuestionByOption(Update update) {
        if (update.hasCallbackQuery()) {
            MaybeInaccessibleMessage maybeMessage = update.getCallbackQuery().getMessage();
            Long chatId = maybeMessage.getChatId();

            if (!chatLanguage.containsKey(chatId))
                setLanguage(chatId, update.getCallbackQuery().getData());

            Option chosenOption = getChosenOption(
                    ((Message) maybeMessage).getText(),
                    update.getCallbackQuery().getData(),
                    chatId,
                    maybeMessage.getMessageId());
            Optional<RedisEntity> currentRedis = redisService.findByChatId(chatId);

            if (currentRedis.isPresent()
                    && currentRedis.get().getCurrentQuestionKey() != null
                    && chosenOption != null) {

                String translatedChosenOption = localizationService.translate(
                        chosenOption.getKey(), chatLanguage.get(chatId));
                currentRedis.get().setLanguage(chatLanguage.get(chatId));
                currentRedis.get().getAnswers().put(currentRedis.get().getCurrentQuestionKey(), translatedChosenOption);

                Optional<Question> nextQuestion = questionService.findById(Objects
                        .requireNonNull(chosenOption).getNextQuestionId());
                if (nextQuestion.isPresent()) {
                    prepareAndSendQuestion(chatId, nextQuestion);
                    currentRedis.get().setCurrentQuestionKey(nextQuestion.get().getKey());
                    redisService.save(currentRedis.get());
                } else if (clientService.getByChatId(chatId).isPresent()) {
                    currentRedis.get().setCurrentQuestionKey(null);
                    redisService.save(currentRedis.get());
                    handleNewSession(chatId);
                } else log.error("No next question and no client with chatId %d in clients".formatted(chatId));
            }
        }
    }

    private void sendQuestionAfterAnswer(long chatId, String answer) {
        Optional<RedisEntity> currentRedis = redisService.findByChatId(chatId);

        if (currentRedis.isPresent() && currentRedis.get().getCurrentQuestionKey() != null) {
            Optional<Question> question = questionService.findByKey(currentRedis.get().getCurrentQuestionKey());

            if (question.isPresent() && !question.get().getOptionList().isEmpty()
                    && question.get().getOptionList().size() == 1) {
                Option nullOption = question.get().getOptionList().get(0);
                Optional<Question> nextQuestion = questionService.findById(nullOption.getNextQuestionId());

                currentRedis.get().setLanguage(chatLanguage.get(chatId));
                currentRedis.get().getAnswers().put(localizationService.findByValue(currentRedis.get().getCurrentQuestionKey()), answer);
                if (nextQuestion.isPresent()) {
                    prepareAndSendQuestion(chatId, nextQuestion);
                    currentRedis.get().setCurrentQuestionKey(nextQuestion.get().getKey());
                    redisService.save(currentRedis.get());
                } else {
                    currentRedis.get().setCurrentQuestionKey(null);
                    redisService.save(currentRedis.get());
                    handleNewSession(chatId);
                }
            }
        }
    }

    private void prepareAndSendQuestion(Long chatId, Optional<Question> nextQuestion) {
        if (nextQuestion.isPresent()) {
            String translatedQuestion = localizationService.translate(nextQuestion.get().getKey(),
                    chatLanguage.get(chatId));
            List<String> translatedOptions = optionService.findByQuestionId(nextQuestion
                            .get().getId()).stream()
                    .map(option -> localizationService.translate(option.getKey(),
                            chatLanguage.get(chatId)))
                    .toList();

            sendQuestion(chatId, translatedQuestion, translatedOptions);
        }
    }

    private Option getChosenOption(String question, String answer, long chatId, int messageId) {
        String optionKey = localizationService.findByValue(answer);
        Optional<Option> chosenOption = optionService.findByKey(optionKey);

        if (chosenOption.isPresent()) {
            String formattedChosenOption = "*" + answer + "*";
            DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatId), messageId);

            try {
                execute(deleteMessage);
                execute(SendMessage.builder().chatId(chatId).text(question + "\nYou chose: " + formattedChosenOption).parseMode(ParseMode.MARKDOWN).build());
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }

            return chosenOption.get();
        } else {
            try {
                execute(SendMessage.builder().chatId(chatId).text("You choose an option").build());
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
                throw new Exception("Unavailable language");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    public boolean checkAnswer(Message message, RedisEntity currentRedis) {
        if (currentRedis==null || currentRedis.getCurrentQuestionKey() == null)
            return false;

        Optional<Question> currentQuestion = questionService.findByKey(currentRedis.getCurrentQuestionKey());

        if (message.hasText() && currentQuestion.isPresent()) {
            String theOptionKey = currentQuestion.get().getOptionList().get(0).getKey();

            if (theOptionKey.equals("dateRange")) {
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
            } else if (theOptionKey.equals("budget")
                    || theOptionKey.equals("count")) {
                try {
                    Long.parseLong(message.getText());
                    return true;
                } catch (NumberFormatException e) {
                    log.error("Cant parse answer to long: " + e.getMessage());
                    return false;
                }
            } else return theOptionKey.equals("freetext");
        }
        return false;
    }

    private void sendQuestion(long chatId, String question, List<String> options) {
        if (question == null)
            log.error("Cannot send question because it is null");
        else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));
            sendMessage.setText(question);

            if (!options.isEmpty() && !options.contains(null)) {
                InlineKeyboardMarkup markupInline = getInlineKeyboardMarkup(options);
                sendMessage.setReplyMarkup(markupInline);
            }

            try {
                execute(sendMessage);
            } catch (TelegramApiException | NullPointerException e) {
                log.error(e.getMessage());
            }
        }
    }

    public void sendWaitingMessageToClient(Long chatId, Language language) {
        if (chatId != null) {
            String waitingMessage = getTranslatedWaitingMessage(language);
            assert waitingMessage != null;
            SendMessage sendMessage = SendMessage.builder().chatId(chatId).text(waitingMessage).build();
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else log.error("Chat id is null - sendWaitingMessageToClient(null,...)");
    }

    private String getTranslatedWaitingMessage(Language language) {
        Optional<BotMessage> botMessage = botMessageService.getBy("waitingMessage", language);
        return botMessage.map(BotMessage::getMessage).orElse(null);
    }

    private void handleNewSession(Long chatId) {
        Optional<Client> client = clientService.getByChatId(chatId);
        Optional<RedisEntity> currentRedis = redisService.findByChatId(chatId);

        if (currentRedis.isPresent() && currentRedis.get().getCurrentQuestionKey() == null) {
            if (client.isPresent()) {
                JSONObject object = new JSONObject();

                currentRedis.get().getAnswers().forEach(object::put);

                Session session = Session.builder()
                        .id(UUID.randomUUID())
                        .client(client.get())
                        .answers(object.toString())
                        .active(true)
                        .registeredAt(LocalDateTime.now())
                        .build();
                sessionService.create(session);
                System.out.println("Session with id: " + session.getId() + " created and is now active");

                // Instead of sending just the JSON object, send the session object
                sendSessionToAnotherApp(session);

                redisService.clearCache();

                sendWaitingMessageToClient(chatId, chatLanguage.get(chatId));
            } else {
                try {
                    throw new Exception("Client does not exist, but quiz is finished");
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        } else {
            try {
                throw new Exception("Current question is not answered, but quiz is finished.");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
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

            List<Session> activeSessions = sessionService.getActiveSessions(chatId);
            if (!activeSessions.isEmpty()) {
                activeSessions.forEach(session -> {
                    session.setActive(false);
                    sessionService.update(session.getId() !=null ? session.getId() : UUID.fromString(""), session);
                });
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

    private void handleDeleteRequest(long chatId) {
        handleStopRequest(chatId);

        List<Session> sessions = sessionService.allSessionsByChatId(chatId);
        if (!sessions.isEmpty())
            sessions.forEach(session -> {
                sessionService.delete(session.getId());
            });

        Optional<Client> client = clientService.getByChatId(chatId);
        client.ifPresent(value -> clientService.delete(value.getClientId()));

        try {
            execute(SendMessage.builder().chatId(chatId).text("Your info deleted successfully!").build());
        } catch (TelegramApiException e) {
            log.error("Couldn't send message. " + e.getMessage());
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
                        new BotCommand("stop", "Stop connection"),
                        new BotCommand("delete", "Delete my data")))
                .build());
    }
}
