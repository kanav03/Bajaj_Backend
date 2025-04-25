package com.bajaj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableRetry
@EnableAsync
@ComponentScan(basePackages = "com.bajaj")
public class WebhookProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebhookProcessorApplication.class, args);
    }
} 