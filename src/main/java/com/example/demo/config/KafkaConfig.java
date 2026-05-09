package com.example.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class KafkaConfig {

    @Value("${kafka.topics.demo:demo-topic}")
    private String demoTopic;

    @Value("${kafka.topics.credit-score-evaluated:credit.score.evaluated}")
    private String creditScoreEvaluatedTopic;

    @Value("${kafka.topics.credit-score-requested:credit.score.requested}")
    private String creditScoreRequestedTopic;

    @Bean
    public NewTopic demoTopic() {
        return TopicBuilder.name(demoTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic creditScoreEvaluatedTopic() {
        return TopicBuilder.name(creditScoreEvaluatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic creditScoreRequestedTopic() {
        return TopicBuilder.name(creditScoreRequestedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /** Dead-letter topic for credit.score.requested failures. */
    @Bean
    public NewTopic creditScoreRequestedDlt() {
        return TopicBuilder.name(creditScoreRequestedTopic + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}

