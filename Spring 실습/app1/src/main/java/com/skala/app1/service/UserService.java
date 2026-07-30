package com.skala.app1.service;

import org.springframework.stereotype.Service;

import com.skala.app1.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service("userService")
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;


    public void printUser() {
        System.out.println("사용자: " + userRepository.findeUser());
    }
}
