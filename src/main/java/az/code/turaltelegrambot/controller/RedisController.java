package az.code.turaltelegrambot.controller;

import az.code.turaltelegrambot.entity.Client;
import az.code.turaltelegrambot.redis.RedisEntity;
import az.code.turaltelegrambot.redis.RedisService;
import az.code.turaltelegrambot.repository.ClientRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
    public ResponseEntity<RedisEntity> getByChatId(@PathVariable Long chatId) {
        Optional<RedisEntity> redisEntity = redisService.findByChatId(chatId);
        return redisEntity.map(entity ->
                new ResponseEntity<>(entity, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
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
