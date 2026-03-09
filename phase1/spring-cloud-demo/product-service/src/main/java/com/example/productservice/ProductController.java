package com.example.productservice;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final UserClient userClient;

    @Value("${product.currency:KRW}")
    private String currency;

    @Value("${product.discount-rate:0.0}")
    private double discountRate;

    public ProductController(UserClient userClient) {
        this.userClient = userClient;
    }

    @GetMapping
    public List<Map<String, Object>> getProducts() {
        return List.of(
            Map.of("id", 1, "name", "Laptop",  "price", 1200, "currency", currency),
            Map.of("id", 2, "name", "Monitor", "price", 350,  "currency", currency)
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProduct(@PathVariable int id) {
        return Map.of("id", id, "name", "Product-" + id, "price", id * 100, "currency", currency);
    }

    @GetMapping("/{id}/detail")
    @CircuitBreaker(name = "user-service", fallbackMethod = "getProductDetailFallback")
    public Map<String, Object> getProductDetail(@PathVariable int id) {
        Map<String, Object> product = Map.of("id", id, "name", "Product-" + id, "price", id * 100, "managerId", id);
        Map<String, Object> user = userClient.getUser(id);

        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("manager", user);
        return result;
    }

    // Circuit Breaker fallback - user-service 호출 실패 시 실행
    public Map<String, Object> getProductDetailFallback(int id, Throwable t) {
        Map<String, Object> product = Map.of("id", id, "name", "Product-" + id, "price", id * 100, "managerId", id);
        Map<String, Object> unknownUser = Map.of("id", 0, "name", "Unknown User", "email", "unknown@fallback.com");

        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("manager", unknownUser);
        return result;
    }

    // Config Server 설정값 확인용 엔드포인트
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of(
            "currency", currency,
            "discountRate", discountRate,
            "source", "Spring Cloud Config Server"
        );
    }
}
