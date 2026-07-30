package com.example.menu.service;

import org.springframework.stereotype.Service;
import com.example.menu.dto.HelloResponse;

@Service
public class HelloServive {

    public String createMessage(String name) {
        HelloResponse response = new HelloResponse();
        response.setMessage("안녕하세요, " + name + "님!");

        return response.getMessage();
    }
}
