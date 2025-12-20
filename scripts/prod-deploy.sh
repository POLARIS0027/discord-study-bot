#!/bin/bash
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

BASE_DIR="/home/ubuntu/discord-study-bot/prod"
CURRENT=$(readlink $BASE_DIR/current 2>/dev/null || echo "blue")
TARGET=""

if [ "$CURRENT" = "blue" ]; then
    TARGET="green"
else
    TARGET="blue"
fi

echo -e "${YELLOW}=== 운영 배포: $TARGET 환경 ===${NC}"
echo "현재 활성: $CURRENT"
echo "배포 대상: $TARGET"
echo ""

cd $BASE_DIR/$TARGET

if [ ! -f "build/libs/discord-study-bot-"*".jar" ]; then
    echo -e "${RED}ERROR: JAR 파일이 없습니다!${NC}"
    echo "로컬에서 빌드 후 업로드하세요:"
    echo "  scp build/libs/*.jar arm:~/discord-study-bot/prod/$TARGET/build/libs/"
    exit 1
fi

echo -e "${YELLOW}=== Docker 이미지 빌드 ===${NC}"
docker compose build

echo ""
echo -e "${GREEN}=== 배포 완료 ===${NC}"
echo "전환 준비 완료. 다음 명령어로 전환하세요:"
echo "  ./scripts/prod-switch.sh"

