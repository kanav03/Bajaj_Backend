package com.bajaj.startup;

import com.bajaj.model.WebhookRequest;
import com.bajaj.model.WebhookResponse;
import com.bajaj.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
public class WebhookStartupProcessor {
    private static final Logger logger = LoggerFactory.getLogger(WebhookStartupProcessor.class);

    @Autowired
    private WebhookService webhookService;

    @EventListener(ApplicationReadyEvent.class)
    public void processWebhookOnStartup() {
        logger.info("Starting webhook processing...");
        
        WebhookRequest request = new WebhookRequest();
        request.setName("Kanav Nijhawan");
        request.setRegNo("RA2211028010100");
        request.setEmail("kn7575@srmist.edu.in");

        logger.debug("Sending webhook request: {}", request);

        webhookService.generateWebhook(request)
                .doOnNext(response -> logger.debug("Received webhook response: {}", response))
                .flatMap(response -> {
                    logger.info("Processing webhook response...");
                    Map<String, Object> result = webhookService.processQuestion(response, request.getRegNo());
                    logger.debug("Generated result: {}", result);
                    
                    return WebClient.builder()
                            .baseUrl(response.getWebhook())
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .defaultHeader(HttpHeaders.AUTHORIZATION, response.getAccessToken())
                            .build()
                            .post()
                            .bodyValue(result)
                            .retrieve()
                            .bodyToMono(String.class)
                            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                            .doOnNext(r -> logger.info("Successfully sent result to webhook"));
                })
                .subscribe(
                    success -> logger.info("Webhook processing completed successfully: {}", success),
                    error -> logger.error("Error processing webhook", error),
                    () -> logger.info("Webhook processing completed")
                );
    }
} 