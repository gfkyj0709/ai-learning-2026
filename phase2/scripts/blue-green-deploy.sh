#!/bin/bash
# Blue/Green Deployment Script
# Blue/Green 무중단 배포 스크립트

set -e

COMPOSE_FILE="/home/younggenius/ai-learning-2026/phase2/docker-compose.yml"
NGINX_CONF="/home/younggenius/ai-learning-2026/phase2/nginx/nginx.conf"

# 현재 활성 서비스 확인 / Check current active service
CURRENT=$(grep "server rag-demo-" $NGINX_CONF | grep -o "rag-demo-[a-z]*")
echo "현재 활성 서비스: $CURRENT"

# 다음 서비스 결정 / Decide next service
if [ "$CURRENT" = "rag-demo-blue" ]; then
    NEXT="rag-demo-green"
    NEXT_PORT="8086"
    PREV="rag-demo-blue"
else
    NEXT="rag-demo-blue"
    NEXT_PORT="8085"
    PREV="rag-demo-green"
fi

echo "새 버전 배포 대상: $NEXT (포트: $NEXT_PORT)"

# 새 버전 이미지 pull / Pull new image
echo "최신 이미지 pull 중..."
docker compose -f $COMPOSE_FILE pull rag-demo-blue rag-demo-green

# 새 버전 컨테이너 시작 / Start new version container
echo "$NEXT 컨테이너 시작..."
if [ "$NEXT" = "rag-demo-green" ]; then
    docker compose -f $COMPOSE_FILE --profile green up -d rag-demo-green
else
    docker compose -f $COMPOSE_FILE up -d rag-demo-blue
fi

# 헬스체크 / Health check
echo "$NEXT 헬스체크 중..."
for i in {1..30}; do
    if curl -sf http://localhost:$NEXT_PORT/actuator/health > /dev/null 2>&1; then
        echo "$NEXT 헬스체크 성공!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "헬스체크 실패! 롤백합니다."
        docker compose -f $COMPOSE_FILE stop $NEXT
        exit 1
    fi
    echo "헬스체크 대기 중... ($i/30)"
    sleep 5
done

# Nginx 설정 전환 / Switch Nginx config
echo "Nginx 트래픽 전환: $PREV → $NEXT"
sed -i "s/server $PREV/server $NEXT/" $NGINX_CONF
docker compose -f $COMPOSE_FILE exec nginx nginx -s reload

# 이전 버전 종료 / Stop previous version
echo "$PREV 종료 중..."
docker compose -f $COMPOSE_FILE stop $PREV

echo "배포 완료! 활성 서비스: $NEXT"