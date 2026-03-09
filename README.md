# AI Learning 2026

개인 AI 엔지니어링 학습 저장소입니다.

---

## Phase 1 - Week 1: LLM 기초 + Spring Boot 연동

**학습 기간:** 2026년 3월

### 학습 목표

로컬 LLM 환경을 직접 구축하고, Spring Boot와 연동하여 실제 동작하는 AI 백엔드를 만든다.

---

### 1. 인프라 구축 — Proxmox Ubuntu VM

- Proxmox VE 위에 Ubuntu Server VM 생성
- CPU / RAM / 디스크 할당 및 네트워크 설정
- SSH 접속 환경 구성

---

### 2. Ollama 설치 및 API 호출

로컬 머신에서 LLM을 직접 실행하는 Ollama를 설치하고 REST API로 호출하는 데 성공했다.

```bash
# Ollama 설치
curl -fsSL https://ollama.com/install.sh | sh

# 모델 다운로드
ollama pull qwen2.5

# REST API 직접 호출
curl http://localhost:11434/api/generate \
  -d '{"model": "qwen2.5", "prompt": "Hello!", "stream": false}'
```

---

### 3. LLM 핵심 개념 정리

| 개념 | 설명 |
|------|------|
| **Token** | LLM이 텍스트를 처리하는 최소 단위. 단어, 단어 조각, 문자 등으로 분할됨 |
| **Temperature** | 출력 무작위성 제어 파라미터. 0에 가까울수록 결정적, 높을수록 창의적 |
| **Top-p (Nucleus Sampling)** | 누적 확률 p 이내의 토큰만 샘플링. Temperature와 함께 출력 다양성 조절 |
| **Context Window** | 모델이 한 번에 처리할 수 있는 최대 토큰 수 |
| **Embedding** | 텍스트를 고차원 벡터로 변환한 표현. 의미적 유사도 계산에 사용 |
| **RAG** | Retrieval-Augmented Generation. 외부 문서를 검색해 LLM 답변 품질을 높이는 패턴 |
| **System Prompt** | 모델의 역할과 행동 방식을 지정하는 사전 지시문 |
| **Fine-tuning** | 특정 도메인 데이터로 모델 가중치를 추가 학습시키는 과정 |

---

### 4. Spring Boot + Ollama 연동

Spring AI를 사용해 Ollama와 연동하는 REST API 서버를 구축했다.

#### 프로젝트 구조

```
phase1/spring-ollama/
├── pom.xml
└── src/main/
    ├── java/com/example/springollama/
    │   ├── SpringOllamaApplication.java
    │   └── ChatController.java
    └── resources/
        └── application.yml
```

#### 핵심 설정 (`application.yml`)

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5

server:
  port: 8081
```

#### API 엔드포인트

```
POST /chat
Content-Type: application/json

{"message": "질문 내용"}
```

#### 호출 예시

```bash
curl -X POST http://localhost:8081/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Java record와 class의 차이점은?"}'
```

---

### 5. Prompt Engineering — 시스템 프롬프트 실습

`ChatClient.Builder`로 시스템 프롬프트를 주입하여 모델의 역할과 응답 스타일을 제어했다.

```java
@PostMapping("/chat")
public ChatResponse chat(@RequestBody ChatRequest request) {
    String answer = chatClient.prompt()
            .system("너는 Java 백엔드 개발 전문가야. 친절하고 간결하게 한국어로 답변해줘.")
            .user(request.message())
            .call()
            .content();
    return new ChatResponse(answer);
}
```

**학습 포인트:**
- 시스템 프롬프트로 모델 페르소나와 응답 언어를 고정할 수 있다
- `ChatClient` 빌더 패턴으로 system / user 메시지를 분리하여 구성한다
- Spring AI가 Ollama REST API 통신을 추상화해 준다

---

### 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 3.4.3 |
| Spring AI | 1.0.0 |
| Build Tool | Maven |
| LLM Runtime | Ollama |
| LLM Model | qwen2.5 |
| 인프라 | Proxmox VE + Ubuntu Server VM |

---

## English Learning Log

### 2026-03-09
- [Fireship] Cloudflare + Next.js 관련 영상
  - URL: https://www.youtube.com/watch?v=abbeIUOCzmw
- [Hacker News] macOS + LLM 활용 관련 글
  - URL: https://agent-safehouse.dev/

---

## 저장소 구조

```
ai-learning-2026/
└── phase1/
    └── spring-ollama/   # Week 1: Spring Boot + Ollama 연동
```
