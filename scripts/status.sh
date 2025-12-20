#!/bin/bash

echo "=========================================="
echo "Discord Study Bot - 전체 상태"
echo "=========================================="

echo ""
echo "=== 개발 환경 ==="
cd /home/ubuntu/discord-study-bot/dev
docker compose ps 2>/dev/null || echo "개발 환경 중지됨"

echo ""
echo "=== 운영 DB ==="
cd /home/ubuntu/discord-study-bot/prod/db
docker compose ps

CURRENT=$(readlink /home/ubuntu/discord-study-bot/prod/current 2>/dev/null || echo "blue")
echo ""
echo "=== 운영 앱 (현재: $CURRENT) ==="

echo ""
echo "Blue:"
cd /home/ubuntu/discord-study-bot/prod/blue
docker compose ps 2>/dev/null | grep prod-app-blue || echo "  중지됨"

echo ""
echo "Green:"
cd /home/ubuntu/discord-study-bot/prod/green
docker compose ps 2>/dev/null | grep prod-app-green || echo "  중지됨"

echo ""
echo "=== 리소스 사용량 ==="
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

echo ""
echo "=== 시스템 리소스 ==="
echo "메모리:"
free -h | grep -E "Mem|Swap"
echo ""
echo "디스크:"
df -h / | tail -1

