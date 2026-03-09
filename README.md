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
    ├── spring-ollama/            # Spring Boot + Ollama 연동 (Docker Compose 포함)
    └── spring-cloud-demo/        # Spring Cloud MSA 전체 실습 (Maven 멀티모듈)
        ├── eureka-server/        #   ├── 서비스 디스커버리 (포트 8761)
        ├── config-server/        #   ├── 중앙 설정 관리 (포트 8888)
        ├── gateway/              #   ├── API 게이트웨이 (포트 8084)
        ├── user-service/         #   ├── 사용자 서비스 (포트 8081)
        └── product-service/      #   └── 상품 서비스 (포트 8082)
```
