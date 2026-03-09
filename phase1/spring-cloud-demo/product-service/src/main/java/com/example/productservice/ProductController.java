package com.example.productservice;

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

    public ProductController(UserClient userClient) {
        this.userClient = userClient;
    }

    @GetMapping
    public List<Map<String, Object>> getProducts() {
        return List.of(
            Map.of("id", 1, "name", "Laptop",  "price", 1200),
            Map.of("id", 2, "name", "Monitor", "price", 350)
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProduct(@PathVariable int id) {
        return Map.of("id", id, "name", "Product-" + id, "price", id * 100);
    }

    @GetMapping("/{id}/detail")
    public Map<String, Object> getProductDetail(@PathVariable int id) {
        Map<String, Object> product = Map.of("id", id, "name", "Product-" + id, "price", id * 100, "managerId", id);
        Map<String, Object> user = userClient.getUser(id);

        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("manager", user);
        return result;
    }
}
