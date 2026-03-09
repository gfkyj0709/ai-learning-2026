package com.example.productservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/users/{id}")
    Map<String, Object> getUser(@PathVariable int id);
}
