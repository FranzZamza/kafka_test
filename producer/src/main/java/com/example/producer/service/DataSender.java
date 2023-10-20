package com.example.producer.service;

import com.example.producer.model.StringValue;

public interface DataSender {
    void send(StringValue value);
}
