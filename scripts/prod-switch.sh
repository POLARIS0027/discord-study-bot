#!/bin/bash
set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_DIR="/home/ubuntu/discord-study-bot/prod"
CURRENT=$(readlink $BASE_DIR/current 2>/dev/null || echo "blue")
TARGET=""

if [ "$CURRENT" = "blue" ]; then
    TARGET="green"
else
    TARGET="blue"
fi

echo -e "${YELLOW}=========================================="
echo "Blue-Green 전환: $CURRENT → $TARGET"
echo "==========================================${NC}"

echo -e "${YELLOW}=== 1. $CURRENT 앱 중지 ===${NC}"
cd $BASE_DIR/$CURRENT
docker compose stop
sleep 5

echo -e "${YELLOW}=== 2. $TARGET 앱 시작 ===${NC}"
cd $BASE_DIR/$TARGET
docker compose up -d
echo "앱 시작 대기 중... (30초)"
sleep 30

echo -e "${YELLOW}=== 3. $TARGET 헬스체크 ===${NC}"
if ! docker compose ps | grep "prod-app-$TARGET" | grep -q "Up"; then
    echo -e "${RED}ERROR: $TARGET 시작 실패! 롤백이 필요합니다.${NC}"
    echo "  ./scripts/prod-rollback.sh"
    exit 1
fi

if docker compose logs | grep -qi "started\|ready"; then
    echo -e "${GREEN}✓ 앱 시작 성공${NC}"
else
    echo -e "${YELLOW}! 로그를 확인하세요${NC}"
fi

echo -e "${YELLOW}=== 4. 심볼릭 링크 업데이트 ===${NC}"
cd $BASE_DIR
rm -f current
ln -s $TARGET current

echo ""
echo -e "${GREEN}=========================================="
echo "전환 완료: $CURRENT → $TARGET"
echo "==========================================${NC}"
echo ""
echo "Discord에서 테스트하세요:"
echo "  - 봇 온라인 확인"
echo "  - !랭킹 명령어"
echo "  - 음성 채널 테스트"
echo ""
echo "문제 발생 시 롤백:"
echo "  ./scripts/prod-rollback.sh"

