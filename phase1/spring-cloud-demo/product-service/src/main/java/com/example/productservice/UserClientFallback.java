package com.example.productservice;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public Map<String, Object> getUser(int id) {
        return Map.of("id", 0, "name", "Unknown User", "email", "unknown");
    }
}
