package com.example.producer.service;

import com.example.producer.model.StringValue;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.function.Consumer;

@Log4j2
public class DataSenderKafka implements DataSender {

    private final KafkaTemplate<String, StringValue> template;
    private final Consumer<StringValue> sendAsk;
    private final String topic;

    public DataSenderKafka(KafkaTemplate<String, StringValue> template,
                           String topic,
                           Consumer<StringValue> sendAsk
    ) {
        this.template = template;
        this.sendAsk = sendAsk;
        this.topic = topic;
    }

    @Override
    public void send(StringValue value) {
        log.info("value:{}", value);
        template.send(topic, value)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("message id:{} was sent, offset:{}", value.id(), result.getRecordMetadata().offset());
                        sendAsk.accept(value);
                    } else {
                        log.error("message id:{} was not sent", value.id(), ex);
                    }
                });
    }
}
