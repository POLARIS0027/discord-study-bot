#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_DIR="/home/ubuntu/discord-study-bot/prod"
CURRENT=$(readlink $BASE_DIR/current 2>/dev/null || echo "blue")
PREVIOUS=""

if [ "$CURRENT" = "blue" ]; then
    PREVIOUS="green"
else
    PREVIOUS="blue"
fi

echo -e "${RED}=========================================="
echo "긴급 롤백: $CURRENT → $PREVIOUS"
echo "==========================================${NC}"

echo -e "${YELLOW}=== 1. $CURRENT 중지 ===${NC}"
cd $BASE_DIR/$CURRENT
docker compose stop
sleep 3

echo -e "${YELLOW}=== 2. $PREVIOUS 시작 ===${NC}"
cd $BASE_DIR/$PREVIOUS
docker compose up -d
sleep 15

echo -e "${YELLOW}=== 3. 심볼릭 링크 복원 ===${NC}"
cd $BASE_DIR
rm -f current
ln -s $PREVIOUS current

echo ""
echo -e "${GREEN}=========================================="
echo "롤백 완료: $CURRENT → $PREVIOUS"
echo "==========================================${NC}"
echo ""
echo "Discord에서 봇 상태를 확인하세요."

