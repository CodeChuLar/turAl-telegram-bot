package az.code.turaltelegrambot.controller;

import az.code.turaltelegrambot.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    private final TelegramBot bootcampBot;
    @PostMapping
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return bootcampBot.onWebhookUpdateReceived(update);
    }

}
