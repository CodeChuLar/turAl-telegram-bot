package az.code.turaltelegrambot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic sessionSender() {
        return TopicBuilder.name("session-new-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic clientSender() {
        return TopicBuilder.name("client-new-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic AcceptOfferSender() {
        return TopicBuilder.name("accept-offer-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
