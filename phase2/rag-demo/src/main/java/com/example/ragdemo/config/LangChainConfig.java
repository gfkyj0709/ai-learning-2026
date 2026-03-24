package com.example.ragdemo.config;

import com.example.ragdemo.agent.FdsAgent;
import com.example.ragdemo.tool.FdsTool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChainConfig {

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.ollama.chat-model.temperature}")
    private double temperature;

    @Value("${langchain4j.ollama.embedding-model.model-name}")
    private String embeddingModelName;

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private int qdrantPort;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    @Value("${langchain4j.ollama.chat-model.log-requests:false}")
    private boolean logRequests;

    @Value("${langchain4j.ollama.chat-model.log-responses:false}")
    private boolean logResponses;

    @Bean
    public ChatModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    // 1.0.0-beta1: 반환 타입을 EmbeddingModel 인터페이스로 선언 (RagService 주입 타입과 통일)
    @Bean
    public EmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 1.0.0-beta1: QdrantEmbeddingStore 빌더 - grpcPort로 명칭 변경
        return QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantPort)
                .collectionName(collectionName)
                .build();
    }

    /**
     * Creates a {@link ContentRetriever} backed by the Qdrant embedding store.
     * Qdrant 임베딩 스토어를 기반으로 하는 ContentRetriever 빈을 생성합니다.
     *
     * <ul>
     *   <li>maxResults: 3 — return at most 3 matching segments per query / 쿼리당 최대 3개 세그먼트 반환</li>
     *   <li>minScore: 0.7 — discard segments with cosine similarity below 0.7 / 코사인 유사도 0.7 미만 세그먼트 제외</li>
     * </ul>
     *
     * @param embeddingStore  the Qdrant-backed vector store / Qdrant 벡터 스토어 빈
     * @param embeddingModel  the Ollama embedding model used to vectorize queries / 쿼리 벡터화에 사용할 Ollama 임베딩 모델
     * @return configured {@link EmbeddingStoreContentRetriever} instance / 설정된 EmbeddingStoreContentRetriever 인스턴스
     */
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                // 유사 문서 최대 반환 개수 / Maximum number of similar documents to return
                .maxResults(3)
                // 최소 유사도 임계값 (0.0 ~ 1.0) / Minimum cosine-similarity threshold (0.0 ~ 1.0)
                .minScore(0.7)
                .build();
    }

    /**
     * Creates the FDS Agent bean backed by Ollama (qwen2.5) with tool-calling and RAG support.
     * Ollama(qwen2.5)와 도구 호출 및 RAG 기능을 갖춘 FDS 에이전트 빈을 생성합니다.
     *
     * <ul>
     *   <li>Model / 모델: {@code qwen2.5} via Ollama at {@code localhost:11434}</li>
     *   <li>Memory / 메모리: {@link MessageWindowChatMemory} — keeps last 10 messages / 최근 10개 메시지 유지</li>
     *   <li>Tools / 도구: {@link FdsTool} — statistics &amp; comparison helpers / 통계·비교 헬퍼</li>
     *   <li>RAG / 검색: {@link ContentRetriever} — retrieves top-3 segments with minScore 0.7 / 유사도 0.7 이상 상위 3개 세그먼트 검색</li>
     * </ul>
     *
     * @param fdsTool          the FDS tool component to register / 등록할 FDS 도구 컴포넌트
     * @param contentRetriever the RAG content retriever / RAG 컨텐츠 검색기
     * @return AiServices-generated {@link FdsAgent} proxy / AiServices가 생성한 FdsAgent 프록시
     */
    @Bean
    public FdsAgent fdsAgent(FdsTool fdsTool, ContentRetriever contentRetriever) {
        // 에이전트 전용 ChatModel — RAG용 모델과 별도로 독립 구성
        // Dedicated ChatModel for the agent — configured independently from the RAG model
        ChatModel agentChatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)           // qwen2.5
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();

        return AiServices.builder(FdsAgent.class)
                .chatModel(agentChatModel)
                // 최근 10개 메시지를 슬라이딩 윈도우로 유지 (대화 문맥 보존)
                // Maintain the last 10 messages in a sliding window (preserves conversation context)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                // FdsTool 등록 — LLM이 필요 시 자동 호출
                // Register FdsTool — automatically called by the LLM when needed
                .tools(fdsTool)
                // RAG 검색기 연결 — 사용자 쿼리를 벡터 검색 후 LLM 컨텍스트에 주입
                // Attach RAG retriever — vector-searches user query and injects results into LLM context
                .contentRetriever(contentRetriever)
                .build();
    }
}
