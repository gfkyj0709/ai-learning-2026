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
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
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
     * src/main/resources/docs/ 하위 .txt 파일을 읽어 청킹 후 Qdrant에 저장
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
     * 질문을 임베딩 → Qdrant 유사 문서 검색 → LLM에 컨텍스트와 함께 질문
     */
    public QueryResult query(String question, int maxResults) {
        log.info("질문 처리 시작: {}", question);

        // 1. 질문 임베딩 (LangChain4j 1.0.0: embed() returns Embedding directly)
        // LangChain4j 1.0.0: embed()가 Embedding을 직접 반환
        Embedding questionEmbedding = embeddingModel.embed(question);
        if (questionEmbedding == null || questionEmbedding.vector().length == 0) {
            log.error("임베딩 결과가 비어있습니다. Ollama 연결 상태를 확인해 주세요. question={}", question);
            throw new IllegalStateException("임베딩 결과가 비어있습니다. Ollama 연결 상태를 확인해 주세요. question=" + question);
        }
        log.debug("임베딩 완료: {}차원", questionEmbedding.vector().length);

        // 2. 유사 문서 검색
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(maxResults)
                .minScore(0.7)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

        log.info("유사 문서 {}건 검색됨", matches.size());

        if (matches.isEmpty()) {
            return new QueryResult(question, "관련 문서를 찾지 못했습니다. 먼저 /rag/ingest로 문서를 등록해 주세요.", List.of());
        }

        // 3. 검색된 문서를 컨텍스트로 구성
        StringBuilder contextBuilder = new StringBuilder();
        List<SourceInfo> sources = new ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            TextSegment segment = match.embedded();
            String source = segment.metadata().getString("source");
            double score = match.score();

            contextBuilder.append(String.format("[문서 %d] (출처: %s, 유사도: %.2f)\n", i + 1, source, score));
            contextBuilder.append(segment.text());
            contextBuilder.append("\n\n");

            sources.add(new SourceInfo(source, score, segment.text()));
        }

        // 4. LLM에 RAG 프롬프트 전송 (1.0.0-beta1: generate() → chat(), Response<AiMessage> → ChatResponse)
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
