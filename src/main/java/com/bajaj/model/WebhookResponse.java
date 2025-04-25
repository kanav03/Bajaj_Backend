package com.bajaj.model;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class WebhookResponse {
    private String webhook;
    private String accessToken;
    private ResponseData data;

    @Getter
    @Setter
    public static class ResponseData {
        private List<User> users;
    }
} 