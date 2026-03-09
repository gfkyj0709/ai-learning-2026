package com.example.springollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = chatClient.prompt()
                .system("너는 Java 백엔드 개발 전문가야. 친절하고 간결하게 한국어로 답변해줘.")
                .user(request.message())
                .call()
                .content();
        return new ChatResponse(answer);
    }

    record ChatRequest(@JsonProperty("message") String message) {}
    record ChatResponse(@JsonProperty("response") String response) {}
}
