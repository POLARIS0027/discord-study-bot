# Discord Study Bot 📚

> Spring Boot와 JDA를 활용한 디스코드 스터디 그룹 관리 봇

스터디 그룹 서버의 구성원들이 음성 채널에서 화면 공유를 통해 공부할 때 자동으로 시간을 기록하고, 랭킹 시스템을 통해 학습 동기를 부여하는 디스코드 봇입니다.

## ✨ 주요 기능

### 📊 자동 공부 시간 기록
- **화면 공유 감지**: 음성 채널에서 화면 공유 시작/종료를 자동으로 감지
- **실시간 추적**: 공부 세션의 시작 시간과 종료 시간을 정확하게 기록
- **주간 분할**: 주가 바뀌는 시점에서 자동으로 기록을 분할하여 정확한 주간 통계 제공
- **데이터베이스 저장**: MySQL을 통한 안정적인 데이터 보관

### 🏆 랭킹 시스템
- **주간 랭킹**: `!주간랭킹` 명령어로 이번 주 공부 시간 순위 확인
- **개인 통계**: `!내랭킹` 명령어로 개인 DM을 통한 상세한 개인 통계 제공
- **자동 발표**: 매주 월요일 오전 10시에 지난주 랭킹을 자동으로 공지

### 🤖 봇 명령어
- `!도움말` - 봇 사용법과 명령어 안내
- `!주간랭킹` - 이번 주 공부 시간 랭킹 표시
- `!내랭킹` - 개인 공부 기록을 DM으로 전송
- `!리제` - 리제쌤 오픈카톡 링크 제공

### ⏰ 자동화 기능
- **스케줄러**: 매주 월요일 자동 랭킹 발표
- **실시간 모니터링**: 음성 채널 입장/퇴장 및 화면 공유 상태 실시간 추적

## 🛠️ 기술 스택

- **Backend**: Spring Boot 3.5.4
- **Discord API**: JDA 5.6.1
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA
- **Build Tool**: Gradle
- **Java Version**: 17
- **Container**: Docker Compose

## 📋 사전 요구사항

- Java 17 이상
- MySQL 8.0 (또는 Docker)
- Discord Bot Token
- Discord 서버 관리자 권한

## 🚀 설치 및 설정

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd discord-study-bot
```

### 2. 데이터베이스 설정 (Docker 사용)
```bash
# .env 파일 생성 후 MySQL 루트 패스워드 설정
echo "MYSQL_ROOT_PASSWORD=your_password" > .env

# MySQL 컨테이너 실행
docker-compose up -d
```

### 3. 애플리케이션 설정
`src/main/resources/application.yml` 파일을 수정하여 다음 정보를 입력하세요:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/study_bot_db
    username: root
    password: your_mysql_password

discord:
  token: YOUR_DISCORD_BOT_TOKEN
  prefix: "!"
  exclude-user-id: "제외할_사용자_ID" # 봇 자신의 ID나 제외하고 싶은 사용자 ID
```

### 4. Discord Bot 설정
1. [Discord Developer Portal](https://discord.com/developers/applications)에서 새 애플리케이션 생성
2. Bot 탭에서 봇 토큰 복사
3. OAuth2 > URL Generator에서 다음 권한 선택:
   - `bot`
   - `Send Messages`
   - `Read Message History`
   - `Connect`
   - `View Channels`
4. 생성된 URL로 봇을 서버에 초대

### 5. 애플리케이션 실행
```bash
# Gradle을 사용한 실행
./gradlew bootRun

# 또는 JAR 파일 빌드 후 실행
./gradlew build
java -jar build/libs/discord-study-bot-0.0.3-SNAPSHOT.jar
```

## 📖 사용법

### 기본 사용법
1. 디스코드 서버의 음성 채널에 입장
2. 화면 공유 시작 → 자동으로 공부 시간 기록 시작
3. 화면 공유 종료 또는 음성 채널 퇴장 → 자동으로 기록 종료

### 명령어 사용법
- 채팅창에 `!도움말` 입력하여 사용 가능한 명령어 확인
- `!주간랭킹`으로 현재 주간 순위 확인
- `!내랭킹`으로 개인 통계를 DM으로 받기

### 자동 랭킹 발표
- 매주 월요일 오전 10시(한국 시간)에 `주간-랭킹` 채널에 지난주 결과 자동 발표
- 해당 이름의 채널이 없으면 랭킹이 발표되지 않습니다

## 🏗️ 프로젝트 구조

```
src/main/java/com/studybot/discord_study_bot/
├── config/
│   └── JdaConfig.java              # JDA 설정
├── dto/
│   └── RankingDto.java             # 랭킹 데이터 전송 객체
├── entity/
│   └── StudyLog.java               # 공부 기록 엔티티
├── listener/
│   ├── RankingCommandListener.java # 명령어 처리 리스너
│   └── VoiceChannelListener.java   # 음성 채널 이벤트 리스너
├── repository/
│   └── StudyLogRepository.java     # 데이터베이스 접근 계층
├── scheduler/
│   └── RankingScheduler.java       # 자동 랭킹 발표 스케줄러
├── service/
│   └── RankingService.java         # 랭킹 비즈니스 로직
└── DiscordStudyBotApplication.java # 메인 애플리케이션
```

## 🔧 주요 기능 상세

### 공부 시간 추적 로직
- 화면 공유 시작 시 `StudyLog` 엔티티 생성 및 시작 시간 기록
- 화면 공유 종료 또는 음성 채널 퇴장 시 종료 시간 기록
- 주가 바뀌는 시점 감지하여 기록을 자동 분할 (정확한 주간 통계를 위함)
- 동시성 처리를 위한 `ConcurrentHashMap` 사용

### 랭킹 시스템
- 주간 단위로 공부 시간 집계
- 상위 10위까지 표시
- 개인 통계는 DM으로 프라이버시 보호
- 서버에서 나간 사용자도 기록 유지

## 🚨 주의사항

- 봇이 정상 작동하려면 음성 채널 관련 권한이 필요합니다
- 개인 랭킹 조회 시 DM이 차단되어 있으면 메시지를 받을 수 없습니다
- 데이터베이스 연결이 끊어지면 진행 중인 공부 세션이 손실될 수 있습니다
- `주간-랭킹` 채널이 없으면 자동 랭킹 발표가 되지 않습니다

## 📝 로깅

- 애플리케이션 로그는 `logs/study-bot.log` 파일에 저장됩니다
- 일별로 로그 파일이 분할되어 관리됩니다
- 주요 이벤트 (공부 시작/종료, 명령어 실행 등)가 모두 기록됩니다