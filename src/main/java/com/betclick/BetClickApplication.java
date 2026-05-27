package com.betclick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BetClickApplication {
    public static void main(String[] args) {
        SpringApplication.run(BetClickApplication.class, args);
    }
}
