package com.example.consumer.service;


import com.example.consumer.model.StringValue;

import java.util.List;

public interface StringValueConsumer {
    void accept(List<StringValue> values);
}
