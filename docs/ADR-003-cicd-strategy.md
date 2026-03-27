# ADR-003: CI/CD 전략 및 배포 환경 설계

## 날짜
2026-03-27

## 상태
결정됨

## 컨텍스트
- 현재 개발 VM과 배포 VM이 동일 (ollama-server, 192.168.219.102)
- 개발 중 직접 실행한 Java 프로세스와 Docker 컨테이너가 같은 포트 충돌 발생
- GitHub Actions Self-hosted Runner가 동일 VM에서 동작 중

## 결정

### 현재 구조 (Phase 2 임시)
```
개발 + 배포 단일 VM (ollama-server)
- 개발 시: Java 프로세스 직접 실행 (테스트용)
- 배포 시: Docker 컨테이너로 실행 (CI/CD)
- 충돌 방지: 배포 전 Java 프로세스 반드시 종료
```

### 목표 구조 (Phase 3 이후)
```
개발 VM (ollama-server)     배포 VM (신규 생성 예정)
- 코드 작성                  - Docker 컨테이너만 실행
- Java 프로세스 직접 실행     - Self-hosted Runner 설치
- 테스트                     - CI/CD 자동 배포 대상
```

### CI/CD 도구 로드맵
- Phase 2 (현재): GitHub Actions + Self-hosted Runner
- Phase 3: Jenkins 추가 학습 (폐쇄망 CI/CD 시나리오)
  - 완전 폐쇄망 환경 재현 (금융/공공기관 실무 대응)
  - GitHub Actions와 동일 파이프라인을 Jenkins로 구현
  - 내부 Git 연동 시나리오

## 배포 전 체크리스트
```bash
# 1. Java 프로세스 점유 포트 확인
sudo lsof -i :8761 -i :8084 -i :8085

# 2. 중복 컨테이너 확인
docker ps -a

# 3. 로컬 빌드 테스트 후 push
mvn clean package -DskipTests
```

## 결과
- GitHub Actions CI/CD 파이프라인 구축 완료
- git push → 자동 빌드 → Docker Hub push → VM 배포 자동화
- 단일 VM 한계로 인한 포트 충돌 이슈 인지 및 운영 규칙 수립

## 향후 계획
- Proxmox에 배포 전용 VM 추가 생성
- Jenkins 학습 및 폐쇄망 CI/CD 시나리오 구현
