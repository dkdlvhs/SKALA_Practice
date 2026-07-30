package com.skala.app1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.skala.app1.service.UserService;

@SpringBootApplication
public class App1Application {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(App1Application.class, args);

		UserService userService = ctx.getBean("userService", UserService.class);
		userService.printUser();
	}

}
