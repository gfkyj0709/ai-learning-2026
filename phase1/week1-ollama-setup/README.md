# Phase 1 - Week 1: Ollama 환경 세팅

## 구성 환경
- Proxmox (Ryzen7 12core, 64GB RAM)
- Ubuntu 24.04 LTS VM (6core, 24GB RAM, 100GB)
- Ollama v0.17.5
- 모델: llama3.2 (3B)

## 아키텍처
맥북 → Tailscale → Proxmox(100.90.81.37) → VM(192.168.219.102:11434)

## 설치 순서

### 1. Ubuntu VM 생성 (Proxmox)
- CPU: 6core, RAM: 24GB, Disk: 100GB
- OS: Ubuntu 24.04.4 LTS Server

### 2. Ollama 설치
curl -fsSL https://ollama.com/install.sh | sh

### 3. 외부 접속 허용
sudo systemctl edit ollama
# 아래 내용 추가
[Service]
Environment="OLLAMA_HOST=0.0.0.0:11434"

sudo systemctl daemon-reload
sudo systemctl restart ollama

### 4. 모델 다운로드
ollama pull llama3.2

### 5. Proxmox 포트포워딩
echo 1 > /proc/sys/net/ipv4/ip_forward
iptables -t nat -A PREROUTING -p tcp --dport 11434 -j DNAT --to-destination 192.168.219.102:11434
iptables -t nat -A POSTROUTING -j MASQUERADE
iptables -t nat -A PREROUTING -i tailscale0 -p tcp --dport 11434 -j DNAT --to-destination 192.168.219.102:11434

## API 테스트
curl http://100.90.81.37:11434/api/generate \
  -d '{
    "model": "llama3.2",
    "prompt": "What is Fraud Detection System?",
    "stream": false
  }'

## 배운 점
- Ollama = 실행 엔진, llama3.2 = AI 모델
- 약어(FDS)보다 풀네임으로 질문해야 정확한 답변
- 프롬프트 품질이 답변 품질을 결정함 (프롬프트 엔지니어링)
