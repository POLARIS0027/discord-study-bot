#!/bin/bash
cd /home/ubuntu/discord-study-bot/dev
echo "=== 개발 환경 시작 ==="
docker compose up -d
echo ""
echo "로그 확인:"
echo "  docker compose logs -f"

