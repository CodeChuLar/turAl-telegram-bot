package az.code.turaltelegrambot.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClientKafkaListener {

    @KafkaListener(topics = "offer-topic", groupId = "telegram-bot")
    public void listen(ConsumerRecord<String, String> record) {
        log.info("Offer Received {}", record.value());

    }
}
