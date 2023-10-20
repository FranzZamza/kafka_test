package com.example.consumer.config;

import com.example.consumer.model.StringValue;
import com.example.consumer.service.StringValueConsumer;
import com.example.consumer.service.StringValueConsumerLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.JacksonUtils;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;


import java.util.List;

import static org.springframework.kafka.support.serializer.JsonDeserializer.TYPE_MAPPINGS;

@Configuration
@Log4j2
public class ApplicationConfig {
    @Value("${application.kafka.topic}")
    private String topicName;

    @Bean
    public ObjectMapper objectMapper() {
        return JacksonUtils.enhancedObjectMapper();
    }

    @Bean
    public ConsumerFactory<String, StringValue> consumerFactory(
            KafkaProperties kafkaProperties, ObjectMapper mapper) {

        var props = kafkaProperties.buildProducerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(TYPE_MAPPINGS, "com.example.producer.model.StringValue:com.example.consumer.model.StringValue");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 3);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 3_000);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<String, StringValue>(props, new StringDeserializer(), new JsonDeserializer<>(mapper));
    }

    @Bean("listenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, StringValue>>
    listenerContainerFactory(ConsumerFactory<String, StringValue> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, StringValue>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setConcurrency(1);
        factory.getContainerProperties().setIdleBetweenPolls(1_000);
        factory.getContainerProperties().setPollTimeout(1_000);

        var executor = new SimpleAsyncTaskExecutor("k-consumer-");
        executor.setConcurrencyLimit(10);
        var listenerTaskExecutor = new ConcurrentTaskExecutor(executor);
        factory.getContainerProperties().setListenerTaskExecutor(listenerTaskExecutor);
        return factory;
    }

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
    }

    @Bean
    public StringValueConsumer stringValueConsumerLogger() {
        return new StringValueConsumerLogger();
    }

    @Bean
    public KafkaClient stringValueConsumer(StringValueConsumer stringValueConsumer) {
        return new KafkaClient(stringValueConsumer);
    }

    public static class KafkaClient {
        private final StringValueConsumer stringValueConsumer;

        public KafkaClient(StringValueConsumer stringValueConsumer) {
            this.stringValueConsumer = stringValueConsumer;
        }

        @KafkaListener(topics = "${application.kafka.topic}",
                containerFactory = "listenerContainerFactory",
                groupId = "${spring.kafka.consumer.group-id}")
        public void listen(@Payload List<StringValue> values) {
            log.info("values, values.size:{}", values.size());
            stringValueConsumer.accept(values);
        }
    }
}
