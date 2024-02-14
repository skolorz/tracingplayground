package com.example.tracingplayground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class TracingplaygroundApplication {

	public static void main(String[] args) {
		SpringApplication.run(TracingplaygroundApplication.class, args);
	}

}
