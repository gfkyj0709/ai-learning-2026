# RAG Demo - Spring Boot 3.4.3 + LangChain4j

FDS 문서 기반 RAG(Retrieval-Augmented Generation) 데모

## 기술 스택
- Spring Boot 3.4.3
- LangChain4j 1.0.0-beta1
- Ollama (qwen2.5 - 채팅, nomic-embed-text - 임베딩)
- Qdrant v1.13.0 (벡터 저장소)

---

## 왜 ChromaDB 대신 Qdrant를 사용하는가?

초기에 ChromaDB를 벡터 저장소로 선택했으나 아래 호환성 문제로 Qdrant로 교체했다.

| 항목 | 내용 |
|------|------|
| **문제** | LangChain4j가 ChromaDB `/api/v1` 엔드포인트 호출 |
| **원인** | ChromaDB 1.x 버전부터 `/api/v2`로 API 변경 |
| **증상** | `405 Method Not Allowed` 에러 발생 |
| **상태** | LangChain4j GitHub Issue #3338, #3649 — 미해결 |
| **결론** | Java 프로젝트에서는 Qdrant 사용 권장 (LangChain4j 공식 지원, Rust 기반 고성능) |

---

## 사전 준비

### 1. Qdrant 실행

```bash
# Qdrant 컨테이너 실행 (v1.13.0 버전 고정 필수 — 아래 주의사항 참고)
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  qdrant/qdrant:v1.13.0

# 컬렉션 생성 (최초 1회 실행)
curl -X PUT http://localhost:6333/collections/fds-documents \
  -H "Content-Type: application/json" \
  -d '{"vectors": {"size": 768, "distance": "Cosine"}}'
```

> ⚠️ **주의사항 — Qdrant 버전을 반드시 v1.13.0으로 고정할 것**
>
> LangChain4j 내부에서 사용하는 Qdrant gRPC 클라이언트 버전이 1.13.0이다.
> Qdrant 서버가 1.14.x 이상이면 클라이언트-서버 버전 불일치로 인해
> 벡터 검색 시 `with_vectors: true` 옵션이 전달되지 않는 버그가 발생한다.
> 이 경우 검색 결과의 벡터값이 빈 배열(0차원)로 반환되어
> `Length of vector a (0) must be equal to the length of vector b (768)` 에러가 발생한다.
> LangChain4j 최신 버전(1.0.0-beta1)에서도 동일한 문제가 확인되었다.

### 2. Ollama 모델 설치

```bash
# 채팅 모델
ollama pull qwen2.5

# 임베딩 모델 (768차원 벡터 생성)
ollama pull nomic-embed-text
```

### 3. 애플리케이션 실행

```bash
mvn spring-boot:run
```

포트: **8085**

---

## API 사용법

### POST /rag/ingest — 문서 임베딩 저장

`src/main/resources/docs/` 하위 `.txt` 파일을 읽어 청킹 후 Qdrant에 저장한다.

```bash
curl -X POST http://localhost:8085/rag/ingest
```

**응답 예시:**
```json
{
  "status": "success",
  "fileCount": 3,
  "totalChunks": 7,
  "message": "처리 파일: fds-overview.txt, fds-rules.txt, fds-ml-model.txt"
}
```

---

### POST /rag/query — RAG 질의응답

```bash
curl -X POST http://localhost:8085/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "FDS 머신러닝 모델 종류가 뭐야?", "maxResults": 3}'
```

**응답 예시:**
```json
{
  "question": "FDS 머신러닝 모델 종류가 뭐야?",
  "answer": "FDS 머신러닝 모델에는 다음과 같은 종류가 있습니다:\n1. 이상치 탐지 모델 - Isolation Forest, Autoencoder, One-Class SVM\n2. 분류 모델 - XGBoost/LightGBM, 랜덤 포레스트, LSTM",
  "sources": [
    {
      "filename": "fds-ml-model.txt",
      "score": 0.837,
      "excerpt": "FDS 머신러닝 모델 설계\n\n1. 사용 모델 종류..."
    }
  ]
}
```

> **참고:** 문서에 없는 내용을 질문하면 `"해당 정보가 문서에 없습니다."` 로 응답한다.
> `sources` 필드의 `score`는 유사도 점수(0~1)이며, 0.7 미만이면 검색 결과에서 제외된다.

---

## 샘플 문서 (docs/)

| 파일 | 내용 |
|------|------|
| `fds-overview.txt` | FDS 시스템 개요 및 구성요소 |
| `fds-rules.txt` | 탐지 규칙 및 시나리오 |
| `fds-ml-model.txt` | 머신러닝 모델 설계 |

---

## RAG 흐름

```
[POST /rag/ingest - 문서 저장]
docs/*.txt 파일 읽기
    → 텍스트 청킹 (500자 단위)
    → nomic-embed-text 임베딩 (768차원 벡터 변환)
    → Qdrant 저장 (gRPC 포트 6334)

[POST /rag/query - 질의응답]
사용자 질문
    → nomic-embed-text 임베딩
    → Qdrant 유사도 검색 (Cosine, 임계값 0.7 이상, 최대 3개)
    → 검색된 문서 컨텍스트 + 질문을 qwen2.5에 전달
    → 자연어 답변 생성
```

---

## 포트 정보

| 포트 | 용도 |
|------|------|
| 8085 | Spring Boot 앱 |
| 6333 | Qdrant REST API (컬렉션 관리, 상태 확인) |
| 6334 | Qdrant gRPC (LangChain4j 내부 통신) |
| 11434 | Ollama (임베딩 + 채팅 모델) |