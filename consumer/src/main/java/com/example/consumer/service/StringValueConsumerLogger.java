package com.example.consumer.service;

import com.example.consumer.model.StringValue;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class StringValueConsumerLogger implements StringValueConsumer {
    @Override
    public void accept(List<StringValue> values) {
        for (var value :
                values) {
            log.info("log:{}", value);
        }
    }
}
