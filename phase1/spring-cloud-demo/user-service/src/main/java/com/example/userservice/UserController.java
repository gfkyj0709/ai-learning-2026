package com.example.userservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Value("${user.greeting:Hello!}")
    private String greeting;

    @Value("${user.max-users:50}")
    private int maxUsers;

    @GetMapping
    public List<Map<String, Object>> getUsers() {
        return List.of(
            Map.of("id", 1, "name", "Alice", "email", "alice@example.com"),
            Map.of("id", 2, "name", "Bob",   "email", "bob@example.com")
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable int id) {
        return Map.of("id", id, "name", "User-" + id, "email", "user" + id + "@example.com");
    }

    // Config Server 설정값 확인용 엔드포인트
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of(
            "greeting", greeting,
            "maxUsers", maxUsers,
            "source", "Spring Cloud Config Server"
        );
    }
}
