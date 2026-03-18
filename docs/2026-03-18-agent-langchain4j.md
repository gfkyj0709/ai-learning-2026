# AI Agent / LangChain4j / Fine-tuning 개념 정리

> 학습일: 2026-03-18
> Phase: 2 (6월 LLM 심화 선행)
> 목표 수준: 다른 사람에게 설명 가능한 수준

---

## 학습 흐름

```
RAG vs Agent → ReAct 루프 → LangChain4j 구조 → Function Calling
→ Pre-training / RLHF / Fine-tuning → Google Colab GPU
```

---

## 1. RAG vs Agent

### 핵심 차이

```
RAG   = 질문 → (필요시 외부 검색) → 답변  (1번 왕복, 고정 흐름)
Agent = 질문 → 판단 → 도구 실행 → 결과 확인
              → 또 판단 → 또 실행 → ... → 최종 답변  (N번 반복, 자율 판단)
```

### RAG의 정확한 정의

벡터DB 전용이 아니라 **외부 정보를 검색해서 LLM에 주입하는 패턴** 전체를 의미함.

```
넓은 의미의 RAG 검색 방식
  벡터DB 검색    → 시맨틱 유사도 기반 (우리가 만든 것)
  키워드 검색    → 전통적인 RDBMS LIKE 검색
  API 호출       → 실시간 날씨, 주가, 뉴스
  파일 읽기      → PDF, Excel 직접 파싱
  웹 검색        → 구글 검색 결과 주입
```

LLM이 이미 알고 있는 질문 → 검색 없이 바로 답변
LLM이 모르는 질문 → 외부 검색 후 주입

### RAG 지식은 모델에 저장되나?

**아니다.** 절대 저장 안 됨.

```
RAG = 오픈북 시험
  매번 Qdrant에서 검색해서 프롬프트에 주입
  대화 끝나면 사라짐
  모델 가중치(벡터 공간) 변화 없음
  동일 질문 100번 해도 매번 벡터DB 조회

Fine-tuning = 책 통째로 암기
  모델 가중치 자체가 변함
  이후엔 검색 없이 답변 가능
```

### RAG vs Agent 관계

```
Agent
  ├── RAG Tool (벡터DB 검색)   ← RAG가 Agent의 부품
  ├── DB Tool (RDBMS 조회)
  ├── 계산 Tool
  └── 웹 검색 Tool

RAG = Agent의 부품 중 하나
Agent = RAG를 포함한 더 큰 구조
```

---

## 2. ReAct 패턴 — Agent 동작 원리

```
Reasoning  →  지금 상황 판단 ("뭘 해야 하지?")
Acting     →  도구 실행 ("DB 조회할게")
Observing  →  결과 확인 ("127건 나왔네")
→ 다시 Reasoning → 반복
```

### Java로 비유

```java
while (!goalAchieved) {
    String thought = llm.think(currentContext);  // Reasoning
    Tool tool = selectTool(thought);              // 도구 선택
    String result = tool.execute();               // Acting
    currentContext.add(result);                   // Observing
    goalAchieved = llm.isGoalMet(currentContext); // 종료 판단
}
```

### "이제 충분하다" 종료 메커니즘

LLM 혼자 판단하는 게 아니라 **두 레이어가 함께 동작**:

```
레이어 1 — Prompt 레벨
  LangChain4j가 시스템 프롬프트에 자동 주입:
  "필요한 정보를 다 모았으면 'Final Answer: ' 로 시작해서 답변해"
  → LLM이 이 지시를 따라 Final Answer: 출력

레이어 2 — 프레임워크 레벨
  LangChain4j가 LLM 출력을 파싱
  → "Final Answer:" 패턴 발견 → 루프 종료
  → maxIterations 초과 → 강제 종료 (무한루프 방지)
```

---

## 3. LangChain4j

### 한 줄 정의

> LLM 애플리케이션을 Java로 만들 때 필요한 공통 기능을 묶어놓은 라이브러리

### 없으면 직접 만들어야 하는 것들

```
Ollama API 호출 / 프롬프트 템플릿 관리 / 대화 히스토리 누적
Tool 정의 → JSON 변환 → LLM 전달 / LLM 응답 파싱
벡터DB 연동 / 임베딩 모델 호출 / RAG 파이프라인
ReAct 루프 구현 / maxIterations 처리 ...
```

### 모듈 구조

```
langchain4j                     ← 핵심 (필수)
langchain4j-ollama              ← Ollama 연동
langchain4j-openai              ← OpenAI 연동
langchain4j-qdrant              ← Qdrant 벡터DB
langchain4j-spring-boot-starter ← Spring 자동설정
```

### 레고 블록 조립 구조

```java
// 블록 1. ChatLanguageModel — LLM 연결
ChatLanguageModel model = OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("qwen2.5")
        .temperature(0.3)
        .build();

// 블록 2. EmbeddingModel — 텍스트 → 벡터 (RAG 때 필요)
EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
        .modelName("nomic-embed-text")
        .build();

// 블록 3. EmbeddingStore — Qdrant 연결 (RAG 때 필요)
EmbeddingStore<TextSegment> store = QdrantEmbeddingStore.builder()
        .host("localhost").port(6334)
        .collectionName("fds-documents")
        .build();

// 블록 4. ChatMemory — 대화 기억
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

// 블록 5. Tool — LLM이 쓸 수 있는 도구
@Tool("이상탐지 건수 조회")
public String getFdsStats(@P("조회 월") String month) { ... }
```

### 용도별 조립 패턴

```java
// A. 단순 챗봇
AiServices.builder(Bot.class)
    .chatLanguageModel(model)
    .chatMemory(memory)
    .build();

// B. RAG
AiServices.builder(RagBot.class)
    .chatLanguageModel(model)
    .contentRetriever(retriever)  // ← RAG 핵심
    .chatMemory(memory)
    .build();

// C. Agent
AiServices.builder(Agent.class)
    .chatLanguageModel(model)
    .tools(fdsTool)               // ← Agent 핵심
    .chatMemory(memory)
    .build();

// D. Agent + RAG (FDS AI Assistant 최종 구조)
AiServices.builder(FdsAgent.class)
    .chatLanguageModel(model)
    .tools(fdsTool)
    .contentRetriever(retriever)
    .chatMemory(memory)
    .build();
```

### Memory — 왜 필요한가

```
메모리 없을 때:
  User: "3월 이상탐지 분석해줘"  → Bot: "127건입니다"
  User: "전월 대비는?"           → Bot: "무슨 전월요?" ← 기억 못함

메모리 있을 때:
  User: "3월 이상탐지 분석해줘"  → Bot: "127건입니다"
  User: "전월 대비는?"           → Bot: "2월 대비 +23건" ← 기억함

MessageWindowChatMemory.withMaxMessages(10)
  → 최근 10개 메시지만 유지
  → 10개 넘으면 오래된 것부터 삭제
  → Context Window 초과 방지
```

---

## 4. Function Calling vs Agent

### 핵심 차이

```
Function Calling (기반 기술)
  LLM이 "이 함수를 이 파라미터로 호출해" JSON 출력
  실행은 개발자 코드가 함
  1번 호출하고 끝 — 루프 없음

Agent (응용 패턴)
  Function Calling을 반복적으로 활용
  매번 결과 보고 다음 Tool 선택 (ReAct 루프)
  목표 달성까지 자율 반복
```

**LangChain4j의 @Tool = Function Calling을 추상화한 것**

```java
@Tool("이상탐지 건수 조회")  // ← 이게 Function Calling
public String getFdsStats(String month) { ... }
```

### 비교표

| | Function Calling | Agent |
|---|---|---|
| 도구 실행 횟수 | 1번 | N번 자동 반복 |
| 루프 | 없음 | ReAct 루프 |
| 다음 행동 판단 | 개발자가 처리 | LLM 자율 판단 |
| 구현 복잡도 | 낮음 | 높음 (프레임워크 필요) |
| 사용 시점 | 단순 1회성 작업 | 다단계 자율 작업 |

---

## 5. Pre-training / RLHF / Fine-tuning

### 전체 흐름

```
Pre-training
  인터넷 전체 텍스트로 "다음 단어 예측" 학습
  → LLM 기본 능력 형성
  → 비용: 수천억 원, 기간: 수개월

RLHF (Reinforcement Learning from Human Feedback)
  인간 평가자가 답변 품질 순위 매김
  → 높은 점수 방향으로 모델 추가 학습
  → "도움이 되고, 안전하고, 정직한" 방향으로 조정
  → ChatGPT, Claude가 대화를 잘 하는 이유

Fine-tuning
  특정 도메인 데이터로 추가 학습
  → 기존 벡터 공간을 도메인 방향으로 미세 조정
  → 비용: 수십만 원~, 기간: 수일~주
```

### GPT-4, Claude는 어떻게 만들어졌나

```
Fine-tuning이 아니라 처음부터 압도적인 규모로 Pre-training
  GPT-4: 추정 1조B+ 파라미터, 수십조 토큰, 수천억 원
  Claude: 비공개, Anthropic 제작

qwen2.5 Fine-tuning ≠ GPT-4 수준
  Fine-tuning = 기존 모델을 특정 방향으로 미세 조정
  모델 크기 자체는 변하지 않음
```

### 우리가 할 수 있는 것

```
Pre-training    → 불가능 (수천억 원)
RLHF            → 맛보기 가능 (Fine-tuning 이후)
Fine-tuning     → Phase 4에서 직접 실습 ← 여기
RAG + Agent     → 지금 하는 것
```

---

## 6. LoRA Fine-tuning + Google Colab

### Google Colab 무료 GPU 스펙

```
GPU: NVIDIA T4 (16GB VRAM) — 가끔 K80 배정
세션: 최대 12시간
비용: 0원, 신용카드 불필요
보장: 없음 (수요 많으면 못 받을 수 있음)
```

### LoRA가 필요한 이유

```
일반 Fine-tuning
  7B 모델 전체 파라미터 업데이트
  → 14GB+ VRAM 필요 → T4 불가

LoRA Fine-tuning
  전체의 1% 미만 파라미터만 업데이트
  → 4~6GB VRAM으로 가능 → T4 무료로 충분
  → 성능 차이는 생각보다 작음
```

### Phase 4 Fine-tuning 계획

```
Step 1. 데이터 준비 (지금부터 조금씩)
  FDS Q&A 100~500개
  {"instruction": "EAI란?", "output": "EAI는 ..."}

Step 2. Google Colab 환경
  Hugging Face 계정 생성 (무료)
  transformers, peft(LoRA), datasets 라이브러리

Step 3. LoRA Fine-tuning 실행
  T4 기준 30분~2시간
  결과: FDS 특화 가중치 파일

Step 4. Ollama로 실행
  .safetensors → GGUF 변환
  → Ollama에 올려서 기존 RAG/Agent와 연동
```

---

## 전체 개념 연결

```
Pre-training  →  LLM 기본 능력 형성
RLHF          →  대화 잘 하도록 조정
Fine-tuning   →  도메인 특화 (Phase 4)
RAG           →  오픈북 시험 (지식 실시간 주입)
Agent         →  RAG 포함한 자율 다단계 처리
LangChain4j   →  위 모든 것을 Java로 조립하는 도구
```

---

## 다음 세션 예정

- [ ] LangGraph (상태 그래프) — Agent보다 정교한 흐름 제어
- [ ] Agent 실습 — rag-demo에 /agent/chat 엔드포인트 추가
- [ ] Prompt Engineering 심화 실습 (Ollama)