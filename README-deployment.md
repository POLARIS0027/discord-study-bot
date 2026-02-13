# Discord Study Bot 배포 가이드

## 📁 디렉토리 구조

```
discord-study-bot/
├── dev/                  # 개발 환경
├── prod/                 # 운영 환경
│   ├── db/              # 운영 DB
│   ├── blue/            # Blue 슬롯
│   ├── green/           # Green 슬롯
│   ├── current -> blue  # 현재 활성
│   └── logs/            # 운영 로그
└── scripts/             # 관리 스크립트
```

## 🚀 개발 환경

### 시작
```bash
./scripts/dev-start.sh
```

### 재빌드
```bash
# 새 JAR 업로드 후
./scripts/dev-rebuild.sh
```

### 중지
```bash
./scripts/dev-stop.sh
```

## 🔵🟢 운영 배포 (Blue-Green)

### 1. 로컬에서 빌드
```bash
./gradlew clean build -x test
```

### 2. JAR 업로드
```bash
# Green으로 배포 (현재 Blue가 활성일 때)
scp -i C:\Users\SJW-DESKTOP\.ssh\ubuntu\shin-dev.key E:\github\studybot\discord-study-bot\build\libs\discord-study-bot-0.0.3-SNAPSHOT.jar ubuntu@138.2.59.61:~/discord-study-bot/prod/green/build/libs/
```

### 3. 배포
```bash
ssh arm
./scripts/prod-deploy.sh
```

### 4. 전환
```bash
./scripts/prod-switch.sh
```

### 5. 롤백 (문제 시)
```bash
./scripts/prod-rollback.sh
```

## 📊 상태 확인

```bash
./scripts/status.sh
```

## 🔍 로그 확인

### 운영 로그
```bash
tail -f ~/discord-study-bot/prod/logs/study-bot.log
```

### Docker 로그
```bash
cd ~/discord-study-bot/prod/blue  # 또는 green
docker compose logs -f
```

## 💡 팁

- Blue/Green은 자동으로 전환됩니다
- 한 번에 하나의 앱만 실행됩니다 (Discord 토큰 제약)
- DB는 공유되므로 데이터 일관성 유지
- 롤백은 언제든지 가능합니다

