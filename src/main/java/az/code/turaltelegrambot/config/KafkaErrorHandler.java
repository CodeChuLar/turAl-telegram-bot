package az.code.turaltelegrambot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

@Slf4j
@RequiredArgsConstructor
public class KafkaErrorHandler implements CommonErrorHandler {

    private final KafkaTemplate<String, Object> errorTemplate;
    private final String groupId;
    @Override
    public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record,
                             Consumer<?, ?> consumer, MessageListenerContainer container) {

        final Throwable cause = thrownException.getCause();
        log.error("{}", cause.getMessage());

        final KafkaErrorMessage<Object> kafkaErrorMessage = KafkaErrorMessage
                .builder()
                .data(record.value())
                .error(cause.getMessage())
                .build();
        errorTemplate.send(getTopic(record), kafkaErrorMessage);
        return true;
    }

    private String getTopic(ConsumerRecord<?, ?> rec) {
        return String.format("%s_%s_ERROR", rec.topic(), groupId);
    }
}
