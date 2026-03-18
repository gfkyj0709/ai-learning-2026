package com.example.ragdemo.controller;

import com.example.ragdemo.agent.FdsAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller that exposes the FDS Agent chat endpoint.
 * FDS 에이전트 채팅 엔드포인트를 제공하는 REST 컨트롤러.
 *
 * <p>Base path: {@code /agent}
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final FdsAgent fdsAgent;

    /**
     * Constructs the controller with the injected {@link FdsAgent}.
     * {@link FdsAgent}를 주입받아 컨트롤러를 생성합니다.
     *
     * @param fdsAgent AiServices-generated agent proxy / AiServices가 생성한 에이전트 프록시
     */
    public AgentController(FdsAgent fdsAgent) {
        this.fdsAgent = fdsAgent;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Request / Response Records
    // 요청 / 응답 레코드
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Request body for the chat endpoint.
     * 채팅 엔드포인트의 요청 바디.
     *
     * @param message user's input message / 사용자 입력 메시지
     */
    public record AgentRequest(String message) {}

    /**
     * Response body from the chat endpoint.
     * 채팅 엔드포인트의 응답 바디.
     *
     * @param reply  agent's response text / 에이전트 응답 텍스트
     * @param status processing status ("success" or "error") / 처리 상태 ("success" 또는 "error")
     */
    public record AgentResponse(String reply, String status) {}

    // ──────────────────────────────────────────────────────────────────────
    // Endpoints
    // 엔드포인트
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Sends a message to the FDS Agent and returns its response.
     * FDS 에이전트에 메시지를 전달하고 응답을 반환합니다.
     *
     * <p>The agent may autonomously call {@code FdsTool} methods (tool-calling)
     * when the user asks for statistics or comparisons.
     * 사용자가 통계나 비교를 요청하면 에이전트가 자율적으로 {@code FdsTool} 메서드를
     * 호출(tool-calling)할 수 있습니다.
     *
     * <pre>
     * POST /agent/chat
     * Content-Type: application/json
     *
     * { "message": "202502 이상탐지 통계 알려줘" }
     * </pre>
     *
     * @param request {@link AgentRequest} containing the user's message
     * @return {@link AgentResponse} with the agent's reply and status
     */
    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@RequestBody AgentRequest request) {
        try {
            String reply = fdsAgent.chat(request.message());
            return ResponseEntity.ok(new AgentResponse(reply, "success"));
        } catch (Exception e) {
            // 에이전트 처리 중 예외 발생 시 에러 응답 반환
            // Return error response if an exception occurs during agent processing
            return ResponseEntity
                    .internalServerError()
                    .body(new AgentResponse("에이전트 처리 중 오류가 발생했습니다: " + e.getMessage(), "error"));
        }
    }
}
