package com.example.menu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import com.example.menu.service.HelloServive;

@RestController
@RequiredArgsConstructor
public class HelloController {

    private final HelloServive helloServive;

    @GetMapping("/hello")
    public String hello(@RequestParam("name") String name) {
        return helloServive.createMessage(name);
    }
}

