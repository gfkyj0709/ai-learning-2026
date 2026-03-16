# AI Learning 2026

개인 AI 엔지니어링 학습 저장소입니다.

---

## Phase 1 - Spring Boot + Spring Cloud + Ollama 연동

**학습 기간:** 2026년 3월

### 학습 목표

로컬 LLM 환경을 직접 구축하고, Spring Boot + Spring Cloud MSA 구조와 연동하여 실제 동작하는 AI 백엔드를 만든다.

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

### 6. Docker 기초 — 이미지, 컨테이너, Dockerfile

Docker의 핵심 개념을 이해하고 멀티 스테이지 빌드로 최적화된 Spring Boot 이미지를 만들었다.

#### 핵심 개념

| 개념 | 설명 |
|------|------|
| **Image** | 컨테이너 실행에 필요한 파일 시스템 스냅샷. 읽기 전용 레이어로 구성 |
| **Container** | 이미지를 실행한 인스턴스. 격리된 프로세스로 동작 |
| **Dockerfile** | 이미지를 빌드하는 명령어 스크립트 |
| **멀티 스테이지 빌드** | 빌드 환경과 실행 환경을 분리해 최종 이미지 크기를 줄이는 기법 |

#### 멀티 스테이지 Dockerfile 패턴

```dockerfile
# Stage 1: 빌드
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw package -DskipTests

# Stage 2: 실행 (JRE만 포함해 이미지 최소화)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**학습 포인트:**
- 빌드 도구(JDK, Maven)는 최종 이미지에 포함할 필요가 없다
- `COPY --from=builder`로 빌드 산출물만 런타임 이미지로 복사한다
- JDK → JRE 전환만으로도 이미지 크기를 대폭 줄일 수 있다

---

### 7. Docker Compose — Spring Boot 컨테이너화

Docker Compose로 Spring Boot 앱을 컨테이너화하고, Ollama와의 통신 방식을 결정하는 아키텍처 판단을 학습했다.

#### Ollama는 VM 직접 설치, Spring Boot만 Docker로 운영하는 이유

Ollama를 Docker 컨테이너로 올리지 않고 VM에 직접 설치한 이유는 두 가지다.

1. **GPU 접근 문제**: Ollama가 GPU를 활용하려면 컨테이너에서 GPU 패스스루 설정이 필요해 복잡도가 높아진다. VM 직접 설치가 훨씬 단순하다.
2. **LLM은 인프라, 앱은 서비스**: Ollama는 DB처럼 항상 떠 있는 인프라 레이어다. Spring Boot 앱만 컨테이너로 관리하면 배포·재시작 단위를 명확하게 분리할 수 있다.

#### 실제 사용한 `docker-compose.yml`

```yaml
services:
  spring-ollama:
    build: .
    ports:
      - "8081:8081"
    environment:
      - SPRING_AI_OLLAMA_BASE_URL=http://192.168.219.102:11434
    restart: unless-stopped
```

#### 주요 명령어

```bash
# 이미지 빌드 후 컨테이너 실행
docker compose up --build

# 백그라운드 실행
docker compose up -d

# 로그 확인
docker compose logs -f

# 컨테이너 중지 및 삭제
docker compose down
```

**학습 포인트:**
- `host.docker.internal`은 컨테이너 내부에서 호스트 머신(VM)의 IP를 가리키는 특수 DNS 이름이다
- Linux에서는 이 이름이 자동으로 해석되지 않아 `extra_hosts: host-gateway`를 명시해야 한다 (Mac은 불필요)
- 환경변수로 `SPRING_AI_OLLAMA_BASE_URL`을 주입하면 application.yml을 수정하지 않고도 환경별로 설정을 분리할 수 있다
- `restart: unless-stopped`로 VM 재부팅 시 자동으로 컨테이너가 되살아난다
- Linux에서는 `host.docker.internal`이 동작 안 해서 VM IP를 직접 사용했다

---

### 8. Spring Cloud MSA 실습

Spring Cloud 전체 컴포넌트를 Maven 멀티모듈 프로젝트로 구성해 MSA 구조를 직접 구현했다.

#### 서비스 구성

| 서비스 | 포트 | 역할 |
|--------|------|------|
| eureka-server | 8761 | 서비스 디스커버리 — 서비스 등록 및 주소 관리 |
| config-server | 8888 | 중앙 설정 관리 — 모든 서비스 설정을 한 곳에서 제공 |
| gateway | 8084 | API 게이트웨이 — 단일 진입점, 라우팅 처리 |
| user-service | 8081 | 사용자 도메인 API (`GET /users`) |
| product-service | 8082 | 상품 도메인 API (`GET /products`) |

#### Maven 멀티모듈 구조

```
phase1/spring-cloud-demo/
├── pom.xml                  # 루트 POM (모듈 목록 정의)
├── eureka-server/           # 서비스 디스커버리
├── config-server/           # 중앙 설정 관리
│   └── src/main/resources/
│       ├── application.yml
│       └── config/
│           ├── user-service.yml
│           └── product-service.yml
├── gateway/                 # API 게이트웨이
├── user-service/            # 사용자 서비스
└── product-service/         # 상품 서비스
```

#### 8-1. Spring Cloud Gateway — API 라우팅

게이트웨이 라우팅 설정 (`gateway/application.yml`):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service       # Eureka 로드밸런싱
          predicates:
            - Path=/users/**

        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/products/**
```

**학습 포인트:**
- 클라이언트는 게이트웨이 포트(8084) 하나만 알면 된다
- `lb://` 접두사로 Eureka 서비스 디스커버리와 자동 연동된다
- 인증·로깅·CORS 같은 공통 관심사를 한 곳에서 처리하기 좋은 위치다

#### 8-2. Spring Cloud Eureka — 서비스 디스커버리

```yaml
# 각 서비스 application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

**학습 포인트:**
- 서비스가 시작되면 Eureka에 자동 등록, 종료되면 자동 해제된다
- Gateway가 `lb://서비스명`으로 Eureka에서 실제 주소를 조회해 라우팅한다
- Eureka 대시보드: `http://localhost:8761`

#### 8-3. Spring Cloud OpenFeign — 서비스 간 통신

인터페이스 선언만으로 다른 마이크로서비스를 호출할 수 있다.

```java
// URL 없이 인터페이스 선언만으로 서비스 간 통신
@FeignClient(name = "user-service")  // Eureka에서 주소 자동 조회
public interface UserClient {
    @GetMapping("/users/{id}")
    Map<String, Object> getUser(@PathVariable int id);
}
```

```bash
# product-service가 user-service를 호출해 데이터를 합쳐서 반환
curl http://localhost:8084/products/1/detail
# → {"product": {...}, "manager": {"id":1, "name":"User-1", "email":"user1@example.com"}}
```

**학습 포인트:**
- `RestTemplate` 방식 대비 URL 조합, 타입 변환, 예외 처리 코드가 사라진다
- JPA Repository처럼 인터페이스 선언만 하면 동작한다
- Eureka와 연동되어 서비스 IP가 바뀌어도 코드 수정이 필요 없다

#### 8-4. Spring Cloud Config Server — 중앙 설정 관리

모든 서비스의 `application.yml` 설정을 Config Server 한 곳에서 관리한다.

```yaml
# config-server/application.yml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config   # 로컬 파일 시스템 방식
```

```yaml
# 각 서비스 application.yml — Config Server에서 설정 가져오기
spring:
  config:
    import: "configserver:http://localhost:8888"
```

```bash
# Config Server에서 설정 조회 API
curl http://localhost:8888/user-service/default
curl http://localhost:8888/product-service/default
```

**학습 포인트:**
- 서비스 시작 시 Config Server에서 설정을 자동으로 가져온다
- 로컬 파일 시스템 외에 GitHub/GitLab 저장소 연동도 가능하다
- `Spring Cloud Bus` + `/actuator/refresh` 조합 시 서비스 재시작 없이 설정 반영 가능 (Phase 2에서 실습 예정)

#### 8-5. Spring Cloud Resilience4j — Circuit Breaker (장애 대응)

연쇄 장애를 방지한다. 하나의 서비스 다운 시 전체 서비스 장애로 번지는 것을 차단한다.

```java
@GetMapping("/{id}/detail")
@CircuitBreaker(name = "userService", fallbackMethod = "getProductDetailFallback")
public Map<String, Object> getProductDetail(@PathVariable int id) {
    Map<String, Object> user = userClient.getUser(id);  // user-service 호출
    // ...
}

// user-service 다운 시 fallback 반환
public Map<String, Object> getProductDetailFallback(@PathVariable int id, Exception e) {
    return Map.of("manager", Map.of("id", 0, "name", "Unknown User", "email", "unknown@fallback.com"));
}
```

```bash
# user-service를 종료한 상태에서 호출해도 정상 응답
curl http://localhost:8084/products/1/detail
# → {"product": {...}, "manager": {"id":0, "name":"Unknown User", "email":"unknown@fallback.com"}}
```

**학습 포인트:**
- `@CircuitBreaker` 어노테이션 하나로 장애 대응 로직을 분리할 수 있다
- user-service가 다운돼도 product-service는 정상 동작을 유지한다
- FDS AI Assistant에서 외부 AI 서비스 장애 시 graceful degradation에 활용 예정

---

### 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 3.4.3 |
| Spring AI | 1.0.0 |
| Spring Cloud | 2024.x |
| Build Tool | Maven |
| LLM Runtime | Ollama |
| LLM Model | qwen2.5 |
| 인프라 | Proxmox VE + Ubuntu Server VM |

---

## Phase 2 - RAG 구현 (선행 학습)

**학습 기간:** 2026년 3월 (Phase 1 완료 후 선행 진행)

### 학습 목표

LangChain4j + Qdrant 벡터 DB를 활용해 RAG(Retrieval-Augmented Generation) 파이프라인을 직접 구현한다.
FDS 문서를 임베딩하고 자연어 질의응답이 동작하는 데모 서비스를 완성한다.

---

### 9. RAG 데모 — LangChain4j + Qdrant + Ollama

#### RAG란?

```
순수 LLM: 학습 데이터만으로 답변 → 도메인 특화 지식 부족
RAG:      외부 문서 검색 → LLM에 컨텍스트 전달 → 정확한 답변
```

#### RAG 파이프라인

```
[문서 저장 단계 - Ingest]
FDS 문서 (.txt)
    → 청크 분할 (500자 단위)
    → nomic-embed-text 임베딩 (768차원 벡터)
    → Qdrant 벡터 DB 저장

[질의응답 단계 - Query]
사용자 질문
    → nomic-embed-text 임베딩
    → Qdrant 유사도 검색 (Cosine, 상위 3개)
    → 검색된 문서 + 질문을 qwen2.5에 전달
    → 자연어 답변 생성
```

#### 프로젝트 구조

```
phase2/rag-demo/
├── pom.xml
└── src/main/
    ├── java/com/example/ragdemo/
    │   ├── RagDemoApplication.java
    │   ├── config/
    │   │   └── LangChainConfig.java      # LangChain4j + Qdrant 빈 설정
    │   ├── controller/
    │   │   └── RagController.java        # REST API 엔드포인트
    │   └── service/
    │       └── RagService.java           # RAG 핵심 로직
    └── resources/
        ├── application.yml               # 포트 8085
        └── docs/
            ├── fds-overview.txt          # FDS 개요 문서
            ├── fds-rules.txt             # FDS 탐지 규칙 문서
            └── fds-ml-model.txt          # FDS ML 모델 설계 문서
```

#### 기술 스택

| 항목 | 버전 / 내용 |
|------|------------|
| LangChain4j | 1.0.0-beta1 |
| Qdrant | v1.13.0 (Docker) |
| 임베딩 모델 | nomic-embed-text (768차원, Ollama) |
| 채팅 모델 | qwen2.5 (Ollama) |
| 포트 | 8085 |

#### Qdrant 실행 방법

```bash
# Qdrant 컨테이너 실행 (v1.13.0 고정 — LangChain4j gRPC 클라이언트 호환 버전)
docker run -d \
  --name qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  qdrant/qdrant:v1.13.0

# 컬렉션 생성 (최초 1회)
curl -X PUT http://localhost:6333/collections/fds-documents \
  -H "Content-Type: application/json" \
  -d '{"vectors": {"size": 768, "distance": "Cosine"}}'
```

> **주의:** LangChain4j 내부 Qdrant gRPC 클라이언트(1.13.0)와 서버 버전이 일치해야 한다.
> Qdrant 최신 버전(1.17.x)과 호환 불일치로 인해 벡터 조회 버그가 발생한다.

#### API 엔드포인트

```
# 문서 임베딩 (최초 1회 또는 문서 변경 시)
POST http://localhost:8085/rag/ingest

# 자연어 질의응답
POST http://localhost:8085/rag/query
Content-Type: application/json

{"question": "질문 내용", "maxResults": 3}
```

#### 호출 예시

```bash
# 1. 문서 임베딩
curl -X POST http://localhost:8085/rag/ingest
# → {"status":"success","fileCount":3,"totalChunks":7}

# 2. 질의응답
curl -X POST http://localhost:8085/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "FDS 머신러닝 모델 종류가 뭐야?", "maxResults": 3}'
# → {"answer":"FDS 머신러닝 모델에는 다음과 같은 종류가 있습니다:\n1. 이상치 탐지 모델...","sources":[...]}
```

**학습 포인트:**
- 문서를 청크로 분할할수록 검색 정확도가 높아진다 (청크 크기는 도메인에 따라 튜닝 필요)
- 유사도 임계값(0.7)으로 관련 없는 문서가 답변에 섞이는 것을 방지한다
- `sources` 필드로 답변의 근거 문서를 추적할 수 있다 (금융권 감사 대응에 유리)
- Qdrant는 REST(6333)와 gRPC(6334) 포트를 모두 열어야 LangChain4j와 정상 동작한다

---

### 10. LLM 심화 — Transformer / Attention / Embedding / Prompt Engineering

**학습일:** 2026-03-16

#### 학습 목표

LLM이 텍스트를 처리하는 내부 원리를 이해하고, 이를 기반으로 효과적인 프롬프트 작성과 RAG/Agent 설계에 활용한다.

#### 10-1. RNN의 한계와 Transformer 등장

| 구분 | RNN | Transformer |
|---|---|---|
| 처리 방식 | 순서대로 (for 루프) | 전체 동시 (행렬 연산) |
| 장기 의존성 | 앞 내용 잊어버림 | 모든 단어가 모든 단어 참조 |
| GPU 활용 | 병렬화 어려움 | 병렬화 최적화 |
| 긴 문장 | 성능 저하 | 성능 유지 |

> 2017년 Google 논문 **"Attention Is All You Need"** — RNN을 완전히 제거하고 Attention만으로 전체 병렬 처리 구현

```java
// RNN 방식 — 순차 처리 (느림)
for (String word : sentence) {
    process(word); // 앞 결과 나와야 다음 처리 가능
}

// Transformer 방식 — 병렬 처리 (빠름)
sentence.parallelStream()
        .map(word -> attention(word, allWords))
        .collect(...);
```

#### 10-2. Attention

**한 줄 정의:** 현재 단어를 이해하기 위해 문장 전체를 조회해서 맥락을 흡수하는 함수

```
Q (Query)  = 내가 찾고 싶은 것 — 현재 처리 중인 단어
K (Key)    = 각 단어의 검색 키 — 모든 단어의 식별자
V (Value)  = 실제 꺼내올 정보 — 각 단어가 가진 내용

Attention(Q, K, V) = softmax(QKᵀ / √d) · V
```

| 수식 요소 | 의미 |
|---|---|
| `QKᵀ` | Query와 모든 Key의 유사도 계산 (내적) |
| `/ √d` | 값 폭발 방지 스케일 조정 |
| `softmax` | 유사도를 0~1 확률로 변환 (합계 = 1) |
| `· V` | 확률 가중치로 Value 합산 → 최종 맥락 벡터 |

```
Before: "해외" = [0.8, 0.7]       ← 단어 자체 의미만
After:  "해외" = [0.763, 0.611]   ← 거래·시간대 맥락까지 반영
```

**Qdrant 연결:** Qdrant의 벡터 유사도 검색이 QKᵀ 내적 연산과 동일한 구조

#### 10-3. Multi-Head Attention

**한 줄 정의:** Attention을 여러 개 병렬로 돌려서 다양한 관점을 동시에 포착하는 구조

| 비교 항목 | Attention | Multi-Head Attention |
|---|---|---|
| 관점 수 | 1개 | 8~96개 |
| 포착 패턴 | 단일 관계 | 복합 조합 관계 |
| 역할 분담 | 없음 | 학습으로 자동 생성 |

FDS 예시:
```
Head 1 → 금액 관점 분석
Head 2 → 위치 관점 분석
Head 3 → 시간 관점 분석
Head 4 → 기기 관점 분석
         ↓
Concat + Linear → 종합 판단
```

> **핵심:** Head 역할 분담은 사람이 지정하는 게 아니라 데이터가 학습 과정에서 자동으로 만들어냄

#### 10-4. Embedding

**한 줄 정의:** 텍스트를 LLM이 처리할 수 있는 숫자 벡터로 변환하는 전처리 게이트웨이

```
의미가 비슷한 단어 = 벡터 거리 가까움
"해외" ↔ "국외" ↔ "외국"  → 가까움
"해외" ↔ "사과"            → 멀음

왕 - 남자 + 여자 = 여왕  (벡터 연산으로 의미 관계 표현)
```

새 단어/개념 학습 방법:

| 방법 | 비용 | 새 정보 반영 |
|---|---|---|
| Pre-training | 수백억 | 완전 내재화 |
| Fine-tuning | 수십만~억 | 도메인 특화 |
| RAG | 거의 0 | 실시간 반영 |

> **RAG = 오픈북 시험** — Qdrant에서 관련 문서 꺼내 보면서 답변 작성. 모델 가중치(벡터) 자체는 변하지 않음

#### 10-5. Prompt Engineering

**핵심 원리:** LLM은 확률 예측 기계 → 프롬프트는 확률 분포를 원하는 방향으로 조작하는 도구

```
프롬프트가 구체적일수록 → 확률 분포가 원하는 방향으로 집중
프롬프트가 모호할수록   → 확률이 사방으로 퍼짐
```

핵심 기법 4가지:

```
1. Role (역할 부여)
   "너는 10년 경력의 금융 보안 전문가야."
   → 해당 역할 벡터 공간 활성화 → 전문적 어휘 확률 상승

2. Few-shot (예시 제공)
   예시 1) 새벽 3시, 해외, 500만원 → 고위험
   예시 2) 오후 2시, 국내, 3만원   → 정상
   → 모델이 예시를 Attention으로 참조해 출력 패턴 맞춤

3. Chain of Thought (단계적 사고)
   "1단계: 시간대 분석 / 2단계: 위치 분석 / ..."
   → 중간 추론이 다음 토큰의 컨텍스트가 됨 → 답변 품질 향상

4. Output Format (출력 형식 지정)
   {"risk_level": "HIGH", "reasons": [], "score": 0}
   → Spring에서 LLM 응답 파싱 시 필수
```

#### 오늘 떠올린 아이디어 — 하이브리드 FDS 구조

```
소액 거래 (10만원 이하)  →  룰 기반 FDS (비용 0, 빠름)
고액 거래 (100만원 이상) →  룰 기반 1차 필터
                          +  Multi-Head Attention 2차 정밀 분석
                             (금액·위치·시간·기기 패턴 동시 탐지)

룰 기반: 사람이 정의한 명시적 패턴
AI 기반: 데이터가 스스로 발견한 복합 패턴 (Impossible Travel 등)
```

→ 상세 내용: [`docs/2026-03-16-llm-core-concepts.md`](docs/2026-03-16-llm-core-concepts.md)

#### 다음 학습 예정

- [ ] AI Agent 동작 원리 (LLM이 스스로 판단해서 도구를 쓰는 구조)
- [ ] LangChain4j Agent / Tool / Memory 구현
- [ ] Prompt Engineering 심화 실습 (Ollama)

---

## 포트 현황

| 포트 | 서비스 | 비고 |
|------|--------|------|
| 8080 | code-server | 브라우저 VS Code |
| 8081 | user-service | Spring Cloud 실습 |
| 8082 | product-service | Spring Cloud 실습 |
| 8084 | gateway | Spring Cloud Gateway |
| 8085 | rag-demo | Phase 2 RAG 데모 |
| 8761 | eureka-server | 서비스 디스커버리 대시보드 |
| 8888 | config-server | 중앙 설정 관리 |
| 6333 | Qdrant REST | 벡터 DB REST API |
| 6334 | Qdrant gRPC | 벡터 DB gRPC (LangChain4j 사용) |
| 11434 | Ollama | LLM 런타임 |

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
├── phase1/
│   ├── spring-ollama/            # Spring Boot + Ollama 연동 (Docker Compose 포함)
│   └── spring-cloud-demo/        # Spring Cloud MSA 전체 실습 (Maven 멀티모듈)
│       ├── eureka-server/        #   ├── 서비스 디스커버리 (포트 8761)
│       ├── config-server/        #   ├── 중앙 설정 관리 (포트 8888)
│       ├── gateway/              #   ├── API 게이트웨이 (포트 8084)
│       ├── user-service/         #   ├── 사용자 서비스 (포트 8081)
│       └── product-service/      #   └── 상품 서비스 (포트 8082)
├── phase2/
│   └── rag-demo/                 # RAG 데모 — LangChain4j + Qdrant + Ollama (포트 8085)
└── docs/
    └── 2026-03-16-llm-core-concepts.md  # LLM 심화 개념 정리
```

## 유지보수 스크립트

### cleanup-codeserver.sh — 비활성 code-server 프로세스 자동 종료

**경로:** `scripts/cleanup-codeserver.sh`

24시간 이상 비활성 상태인 code-server extensionHost 프로세스를 자동으로 종료한다.
code-server 브라우저 탭을 닫지 않고 방치하면 탭당 ~200MB씩 메모리를 점유하는 문제를 방지한다.

#### crontab 등록 (최초 1회)
```bash
crontab -e
```
아래 내용 추가:
```
0 * * * * /home/younggenius/scripts/cleanup-codeserver.sh >> /home/younggenius/scripts/cleanup.log 2>&1
```

#### 로그 확인
```bash
cat ~/scripts/cleanup.log
```