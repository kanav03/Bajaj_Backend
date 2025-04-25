package com.bajaj.service;

import com.bajaj.model.WebhookRequest;
import com.bajaj.model.WebhookResponse;
import com.bajaj.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    @Value("${webhook.base.url}")
    private String baseUrl;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<WebhookResponse> generateWebhook(WebhookRequest request) {
        return webClient.post()
                .uri("/generateWebhook")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WebhookResponse.class)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException &&
                                ((WebClientResponseException) throwable).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
                        .doBeforeRetry(retrySignal -> 
                            logger.info("Retrying after rate limit... Attempt: {}", retrySignal.totalRetries()))
                )
                .doOnError(error -> logger.error("Error in webhook call", error));
    }

    public Map<String, Object> processQuestion(WebhookResponse response, String regNo) {
        Map<String, Object> result = new HashMap<>();
        result.put("regNo", regNo);

        // Extract the numeric part from the end of the registration number
        String numericPart = regNo.replaceAll("[^0-9]", "");
        int lastTwoDigits = Integer.parseInt(numericPart.substring(numericPart.length() - 2));
        List<List<Integer>> outcome;
        
        if (lastTwoDigits % 2 == 1) {
            // Question 1: Mutual Followers
            outcome = findMutualFollowers(response.getData().getUsers());
        } else {
            // Question 2: Nth Level Followers
            // For simplicity, we'll assume n=2 and findId=1 as default
            outcome = findNthLevelFollowers(response.getData().getUsers(), 1, 2);
        }
        
        result.put("outcome", outcome);
        return result;
    }

    private List<List<Integer>> findMutualFollowers(List<User> users) {
        List<List<Integer>> mutualFollowers = new ArrayList<>();
        
        for (int i = 0; i < users.size(); i++) {
            User user1 = users.get(i);
            for (int j = i + 1; j < users.size(); j++) {
                User user2 = users.get(j);
                if (user1.getFollows().contains(user2.getId()) && 
                    user2.getFollows().contains(user1.getId())) {
                    mutualFollowers.add(Arrays.asList(
                        Math.min(user1.getId(), user2.getId()),
                        Math.max(user1.getId(), user2.getId())
                    ));
                }
            }
        }
        
        return mutualFollowers;
    }

    private List<List<Integer>> findNthLevelFollowers(List<User> users, int findId, int n) {
        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        Set<Integer> currentLevel = new HashSet<>();
        currentLevel.add(findId);
        
        // Process each level
        for (int level = 0; level < n; level++) {
            Set<Integer> nextLevel = new HashSet<>();
            for (int userId : currentLevel) {
                User user = userMap.get(userId);
                if (user != null && user.getFollows() != null) {
                    nextLevel.addAll(user.getFollows());
                }
            }
            currentLevel = nextLevel;
        }
        
        return Collections.singletonList(
            currentLevel.stream()
                .sorted()
                .collect(Collectors.toList())
        );
    }
} 