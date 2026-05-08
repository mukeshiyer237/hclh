package com.example.demo.kafka;

import com.example.demo.kafka.producer.KafkaProducerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock KafkaTemplate<String, String> kafkaTemplate;

    private KafkaProducerService producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        producer = new KafkaProducerService(kafkaTemplate, objectMapper);
    }

    @Test
    void publish_sendsSerializedJsonToCorrectTopic() {
        when(kafkaTemplate.send(anyString(), any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publish("my-topic", "key1", new TestPayload("hello"));

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("my-topic"), eq("key1"), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).contains("\"value\":\"hello\"");
    }

    @Test
    void publish_withoutKey_sendsWithNullKey() {
        when(kafkaTemplate.send(anyString(), isNull(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publish("my-topic", new TestPayload("no-key"));

        verify(kafkaTemplate).send(eq("my-topic"), isNull(), anyString());
    }

    @Test
    void publish_failedSerialization_doesNotCallKafkaTemplate() {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        try {
            when(brokenMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("fail") {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        KafkaProducerService brokenProducer = new KafkaProducerService(kafkaTemplate, brokenMapper);
        assertThatNoException().isThrownBy(() -> brokenProducer.publish("topic", "payload"));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publish_kafkaSendFailure_doesNotThrow() {
        CompletableFuture<Object> failed = CompletableFuture.failedFuture(new RuntimeException("broker down"));
        when(kafkaTemplate.send(anyString(), any(), anyString()))
                .thenReturn((CompletableFuture) failed);

        assertThatNoException().isThrownBy(
                () -> producer.publish("topic", "key", new TestPayload("x")));
    }

    @Test
    void publish_serializesComplexObjectCorrectly() throws JsonProcessingException {
        when(kafkaTemplate.send(anyString(), any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publish("events", "k", new TestPayload("complex"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("events"), eq("k"), captor.capture());

        TestPayload deserialized = objectMapper.readValue(captor.getValue(), TestPayload.class);
        assertThat(deserialized.value()).isEqualTo("complex");
    }

    record TestPayload(String value) {}
}
