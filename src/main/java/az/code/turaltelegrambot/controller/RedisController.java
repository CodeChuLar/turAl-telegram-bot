package az.code.turaltelegrambot.controller;

import az.code.turaltelegrambot.entity.Client;
import az.code.turaltelegrambot.redis.RedisEntity;
import az.code.turaltelegrambot.redis.RedisService;
import az.code.turaltelegrambot.repository.ClientRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {
    private final RedisService redisService;
    private final ClientRepo userRepo;

    @GetMapping
    public List<RedisEntity> getAll() {
        return redisService.getAll();
    }

    @GetMapping("/{chatId}")
    public RedisEntity getByChatId(@PathVariable Long chatId) {
        return redisService.findByChatId(chatId).get();
    }

    @GetMapping("/getall")
    public List<Client> getAllUsers() {
        return userRepo.findAll();
    }

    @DeleteMapping
    public String deleteAll() {
        redisService.clearCache();
        return "Cache cleared";
    }
}
