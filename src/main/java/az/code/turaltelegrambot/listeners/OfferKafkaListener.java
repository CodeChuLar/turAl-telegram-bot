package az.code.turaltelegrambot.listeners;

import az.code.turaltelegrambot.dto.OfferDto;
import az.code.turaltelegrambot.service.OfferService;
import az.code.turaltelegrambot.telegram.TelegramBot;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OfferKafkaListener {

    private final ObjectMapper objectMapper;
    private final OfferService offerService;

    @KafkaListener(topics = "offer-topic", groupId = "telegram-bot")
    public void listen(ConsumerRecord<String, String> record) {
        String jsonString = record.value();
        log.info("Received Kafka message: {}", jsonString);
        try {
            OfferDto offerDto = objectMapper.readValue(jsonString, OfferDto.class);
            offerService.generateImageWithText(offerDto);
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", e.getMessage());
        }
    }
}
