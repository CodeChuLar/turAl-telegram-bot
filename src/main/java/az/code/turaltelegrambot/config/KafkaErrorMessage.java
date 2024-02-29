package az.code.turaltelegrambot.config;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class KafkaErrorMessage<T> {
    T data;
    String error;
}
