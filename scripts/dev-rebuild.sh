#!/bin/bash
cd /home/ubuntu/discord-study-bot/dev
echo "=== 개발 환경 재빌드 ==="
docker compose down
docker compose build --no-cache
docker compose up -d
echo ""
echo "로그 확인:"
echo "  docker compose logs -f"

