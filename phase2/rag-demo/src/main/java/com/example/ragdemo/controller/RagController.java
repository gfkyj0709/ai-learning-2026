package com.example.ragdemo.controller;

import com.example.ragdemo.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * POST /rag/ingest
     * docs/ 하위 .txt 파일을 읽어 청킹 후 ChromaDB에 임베딩 저장
     */
    @PostMapping("/ingest")
    public ResponseEntity<?> ingest() {
        try {
            log.info("문서 인제스트 요청 수신");
            RagService.IngestResult result = ragService.ingestDocuments();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "fileCount", result.fileCount(),
                    "totalChunks", result.totalChunks(),
                    "message", result.message()
            ));
        } catch (IOException e) {
            log.error("문서 인제스트 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "문서 읽기 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /rag/query
     * Body: { "question": "...", "maxResults": 3 }
     */
    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody QueryRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "question 필드가 필요합니다."
            ));
        }

        int maxResults = (request.maxResults() != null && request.maxResults() > 0)
                ? request.maxResults() : 3;

        log.info("RAG 질의 요청: question={}, maxResults={}", request.question(), maxResults);

        RagService.QueryResult result = ragService.query(request.question(), maxResults);

        return ResponseEntity.ok(Map.of(
                "question", result.question(),
                "answer", result.answer(),
                "sources", result.sources()
        ));
    }

    public record QueryRequest(String question, Integer maxResults) {}
}
