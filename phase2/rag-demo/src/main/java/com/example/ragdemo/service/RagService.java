package com.example.ragdemo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * Ingests .txt files from src/main/resources/docs/ into Qdrant.
     * docs/ 하위 .txt 파일을 읽어 청킹 후 Qdrant에 저장한다.
     */
    public IngestResult ingestDocuments() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:docs/*.txt");

        if (resources.length == 0) {
            return new IngestResult(0, 0, "docs/ 디렉토리에 .txt 파일이 없습니다.");
        }

        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        int fileCount = 0;
        int totalChunks = 0;
        List<String> processedFiles = new ArrayList<>();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            String content = resource.getContentAsString(StandardCharsets.UTF_8);

            log.info("문서 임베딩 시작: {}", filename);

            Metadata metadata = Metadata.metadata("source", filename);
            Document document = Document.from(content, metadata);

            List<TextSegment> segments = splitter.split(document);
            ingestor.ingest(document);

            fileCount++;
            totalChunks += segments.size();
            processedFiles.add(filename);

            log.info("문서 임베딩 완료: {} ({} 청크)", filename, segments.size());
        }

        String message = String.format("처리 파일: %s", String.join(", ", processedFiles));
        return new IngestResult(fileCount, totalChunks, message);
    }

    /**
     * Embeds the question, searches Qdrant via REST API, and asks the LLM with context.
     * 질문을 임베딩 → Qdrant REST API로 유사 문서 검색 → LLM에 컨텍스트와 함께 질문
     *
     * @param question  User question / 사용자 질문
     * @param maxResults Max number of similar documents / 최대 검색 문서 수
     */
    public QueryResult query(String question, int maxResults) throws Exception {
        log.info("질문 처리 시작: {}", question);

        // 1. Embed the question / 질문 임베딩
        Response<Embedding> embeddingResponse = embeddingModel.embed(question);
        Embedding questionEmbedding = embeddingResponse.content();
        if (questionEmbedding == null || questionEmbedding.vector().length == 0) {
            log.error("임베딩 결과가 비어있습니다. question={}", question);
            throw new IllegalStateException("임베딩 결과가 비어있습니다.");
        }
        log.info("임베딩 완료: {}차원", questionEmbedding.vector().length);

        // 2. Search Qdrant via REST API (workaround for LangChain4j with_vectors bug)
        // Qdrant REST API 직접 호출 (LangChain4j with_vectors 버그 우회)
        float[] vector = questionEmbedding.vector();
        StringBuilder vectorJson = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            vectorJson.append(vector[i]);
            if (i < vector.length - 1) vectorJson.append(",");
        }
        vectorJson.append("]");

        String requestBody = String.format(
            "{\"vector\": %s, \"limit\": %d, \"with_payload\": true, \"with_vector\": false}",
            vectorJson, maxResults
        );

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://qdrant:6333/collections/fds-documents/points/search"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        java.net.http.HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant 검색 중 인터럽트 발생", e);
        }

        log.info("Qdrant 응답 코드: {}", httpResponse.statusCode());

        // 3. Parse Qdrant response / Qdrant 응답 파싱
        String responseBody = httpResponse.body();
        List<SourceInfo> sources = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
        com.fasterxml.jackson.databind.JsonNode results = root.path("result");

        if (results.isEmpty()) {
            return new QueryResult(question, "관련 문서를 찾지 못했습니다. 먼저 /rag/ingest로 문서를 등록해 주세요.", List.of());
        }

        for (int i = 0; i < results.size(); i++) {
            com.fasterxml.jackson.databind.JsonNode point = results.get(i);
            double score = point.path("score").asDouble();
            String text = point.path("payload").path("text_segment").asText();
            String source = point.path("payload").path("source").asText();

            contextBuilder.append(String.format("[문서 %d] (출처: %s, 유사도: %.2f)\n", i + 1, source, score));
            contextBuilder.append(text);
            contextBuilder.append("\n\n");

            sources.add(new SourceInfo(source, score, text));
        }

        log.info("유사 문서 {}건 검색됨", sources.size());

        // 4. Send RAG prompt to LLM / LLM에 RAG 프롬프트 전송
        String prompt = String.format("""
                다음 참고 문서를 기반으로 질문에 답변해 주세요.
                참고 문서에 없는 내용은 "해당 정보가 문서에 없습니다"라고 답변해 주세요.

                [참고 문서]
                %s

                [질문]
                %s

                [답변]
                """, contextBuilder.toString(), question);

        ChatResponse response = chatLanguageModel.chat(UserMessage.from(prompt));
        String answer = response.aiMessage().text();

        log.info("LLM 답변 완료");
        return new QueryResult(question, answer, sources);
    }

    // DTO records
    public record IngestResult(int fileCount, int totalChunks, String message) {}
    public record QueryResult(String question, String answer, List<SourceInfo> sources) {}
    public record SourceInfo(String filename, double score, String excerpt) {}
}
