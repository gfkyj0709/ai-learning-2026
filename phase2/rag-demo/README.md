# RAG Demo - Spring Boot 3.4.3 + LangChain4j

FDS 문서 기반 RAG(Retrieval-Augmented Generation) 데모

## 기술 스택
- Spring Boot 3.4.3
- LangChain4j 0.36.2
- Ollama (qwen2.5 - 채팅, nomic-embed-text - 임베딩)
- ChromaDB (벡터 저장소)

## 사전 준비

### 1. ChromaDB 실행
```bash
docker-compose up -d
```

### 2. Ollama 모델 설치
```bash
# 채팅 모델
ollama pull qwen2.5

# 임베딩 모델
ollama pull nomic-embed-text
```

### 3. 애플리케이션 실행
```bash
mvn spring-boot:run
```
포트: **8085**

---

## API 사용법

### POST /rag/ingest - 문서 임베딩 저장
`src/main/resources/docs/` 하위 `.txt` 파일을 읽어 청킹 후 ChromaDB에 저장합니다.

```bash
curl -X POST http://localhost:8085/rag/ingest
```

**응답 예시:**
```json
{
  "status": "success",
  "fileCount": 3,
  "totalChunks": 21,
  "message": "처리 파일: fds-overview.txt, fds-rules.txt, fds-ml-model.txt"
}
```

---

### POST /rag/query - RAG 질의응답
```bash
curl -X POST http://localhost:8085/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "FDS에서 새벽 거래는 어떻게 처리하나요?", "maxResults": 3}'
```

**응답 예시:**
```json
{
  "question": "FDS에서 새벽 거래는 어떻게 처리하나요?",
  "answer": "새벽 00시~04시 사이 50만원 이상 이체가 발생하면 추가 인증을 요구합니다...",
  "sources": [
    {
      "filename": "fds-rules.txt",
      "score": 0.91,
      "excerpt": "새벽 00시~04시 사이 50만원 이상 이체 발생 시..."
    }
  ]
}
```

---

## 샘플 문서 (docs/)
| 파일 | 내용 |
|------|------|
| `fds-overview.txt` | FDS 시스템 개요 및 구성요소 |
| `fds-rules.txt` | 탐지 규칙 및 시나리오 |
| `fds-ml-model.txt` | 머신러닝 모델 설계 |

## RAG 흐름
```
[POST /rag/ingest]
문서 파일 읽기 → 텍스트 청킹(500자/50자 오버랩) → 임베딩(nomic-embed-text) → ChromaDB 저장

[POST /rag/query]
질문 임베딩 → ChromaDB 유사도 검색(Top-K) → 컨텍스트 구성 → qwen2.5 LLM 답변 생성
```
