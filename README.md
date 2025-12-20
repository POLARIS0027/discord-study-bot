# Discord Study Bot 📚

> Spring Boot와 JDA를 활용한 멀티 서버 지원 디스코드 스터디 그룹 관리 봇

스터디 그룹 서버의 구성원들이 음성 채널에서 화면 공유를 통해 공부할 때 자동으로 시간을 기록하고, 랭킹 시스템을 통해 학습 동기를 부여하는 디스코드 봇입니다.

## ✨ 주요 기능

### 📊 자동 공부 시간 기록
- **화면 공유 감지**: 음성 채널에서 화면 공유 시작/종료를 자동으로 감지
- **실시간 추적**: 공부 세션의 시작 시간과 종료 시간을 정확하게 기록
- **주간 분할**: 주가 바뀌는 시점에서 자동으로 기록을 분할하여 정확한 주간 통계 제공
- **멀티 서버 지원**: 여러 Discord 서버에서 독립적으로 작동하며 서버별로 데이터 분리
- **동시 활동 지원**: 한 사용자가 여러 서버에서 동시에 화면공유 가능
- **데이터베이스 저장**: MySQL을 통한 안정적인 데이터 보관

### 🏆 랭킹 시스템
- **서버별 독립 랭킹**: 각 서버마다 독립적인 랭킹 시스템
- **주간 랭킹**: `!주간랭킹` 또는 `!週間ランキング` 명령어로 이번 주 공부 시간 순위 확인
- **개인 통계**: `!내랭킹` 또는 `!マイランキング` 명령어로 개인 DM을 통한 상세한 개인 통계 제공
- **이벤트 랭킹**: `!이벤트` 또는 `!イベント` 명령어로 이벤트 기간 누계 랭킹 확인
- **자동 발표**: 매주 월요일 오전 10시에 지난주 랭킹을 각 서버의 `주간-랭킹` 채널에 자동 공지

### 🌍 다국어 지원
- **한국어/일본어 이중 커맨드**: 한국어와 일본어 명령어 모두 지원
- **자동 언어 감지**: 사용한 명령어에 맞춰 응답 메시지 언어 자동 변경
- **지원 명령어**:
  - 도움말: `!도움말` / `!ヘルプ`
  - 주간랭킹: `!주간랭킹` / `!週間ランキング`
  - 내랭킹: `!내랭킹` / `!マイランキング`
  - 이벤트: `!이벤트` / `!イベント`
  - 리제: `!리제` / `!リゼ`

### 🤖 봇 명령어
**한국어 명령어**:
- `!도움말` - 봇 사용법과 명령어 안내
- `!주간랭킹` - 이번 주 공부 시간 랭킹 표시 (해당 서버)
- `!내랭킹` - 개인 공부 기록을 DM으로 전송
- `!이벤트` - 이벤트 기간 누계 랭킹 표시
- `!리제` - 리제쌤 오픈카톡 링크 제공

**日本語コマンド**:
- `!ヘルプ` - ボット使用方法とコマンド案内
- `!週間ランキング` - 今週の勉強時間ランキング表示
- `!マイランキング` - 個人勉強記録をDMで送信
- `!イベント` - イベント期間累計ランキング表示
- `!リゼ` - リゼ先生オープンチャットリンク提供

### ⏰ 자동화 기능
- **스케줄러**: 매주 월요일 각 서버별 자동 랭킹 발표
- **실시간 모니터링**: 음성 채널 입장/퇴장 및 화면 공유 상태 실시간 추적

### 📈 웹 통계 API
향후 웹 프론트엔드 연동을 위한 REST API 제공:
- **개인 통계**: `/api/statistics/personal/{guildId}/{userId}` - 일별/주별/월별 공부 시간
- **서버 랭킹**: `/api/statistics/ranking/{guildId}` - 서버별 주간 랭킹
- **Contribution 히트맵**: `/api/statistics/contribution/{guildId}/{userId}` - GitHub 스타일 일별 공부 기록 (최근 1년)
- **히트맵**: `/api/statistics/heatmap/{guildId}/{userId}` - 시간대/요일별 공부 패턴
- **연속 기록**: `/api/statistics/streak/{guildId}/{userId}` - 연속 공부 일수 추적
- **이벤트 랭킹**: `/api/statistics/event-ranking/{guildId}` - 이벤트 기간 랭킹

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

# 기존 데이터 마이그레이션이 필요한 경우에만 설정
migration:
  legacy-guild-id: "" # 기존 서버의 Discord Guild ID
  legacy-guild-name: "" # 기존 서버 이름

# 이벤트 기간 설정
event:
  start-date: "2025-10-01"
  end-date: "2025-12-31"
```

### 3.1. 기존 데이터 마이그레이션 (선택사항)
이전 버전에서 업그레이드하는 경우, 기존 데이터에 서버 정보를 추가해야 합니다:

1. `application.yml`에 기존 서버 정보 입력:
```yaml
migration:
  legacy-guild-id: "1234567890"  # 기존 서버의 Guild ID
  legacy-guild-name: "리제쌤 스터디"  # 기존 서버 이름
```

2. 애플리케이션 시작 시 자동으로 NULL인 `guild_id`가 업데이트됩니다.

또는 수동으로 SQL 실행:
```sql
UPDATE study_log 
SET guild_id = '기존서버ID', guild_name = '기존서버이름' 
WHERE guild_id IS NULL;
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
│   ├── JdaConfig.java              # JDA 설정
│   └── CommandConfig.java          # 다국어 커맨드 매핑 설정
├── controller/
│   └── StatisticsController.java  # 웹 통계 REST API
├── dto/
│   ├── RankingDto.java             # 랭킹 데이터 전송 객체
│   ├── PersonalStatsDto.java       # 개인 통계 DTO
│   ├── ContributionHeatmapDto.java # GitHub 스타일 Contribution 히트맵 DTO
│   ├── HeatmapDto.java             # 시간대별 히트맵 DTO
│   └── StreakDto.java              # 연속 기록 DTO
├── entity/
│   └── StudyLog.java               # 공부 기록 엔티티 (guildId, guildName 포함)
├── i18n/
│   └── MessageProvider.java        # 다국어 메시지 관리
├── listener/
│   ├── RankingCommandListener.java # 다국어 명령어 처리 리스너
│   └── VoiceChannelListener.java   # 음성 채널 이벤트 리스너 (멀티 서버 지원)
├── repository/
│   └── StudyLogRepository.java     # 데이터베이스 접근 계층
├── scheduler/
│   └── RankingScheduler.java       # 서버별 자동 랭킹 발표 스케줄러
├── service/
│   ├── RankingService.java         # 랭킹 비즈니스 로직
│   ├── StatisticsService.java      # 웹 통계 비즈니스 로직
│   └── DataMigrationService.java   # 기존 데이터 마이그레이션
└── DiscordStudyBotApplication.java # 메인 애플리케이션
```

## 🔧 주요 기능 상세

### 멀티 서버 지원
- **서버별 독립 데이터**: 각 Discord 서버(Guild)의 데이터가 완전히 분리되어 저장
- **서버 식별**: `guildId`와 `guildName`을 모든 기록에 포함
- **동시 활동 지원**: 한 사용자가 여러 서버에서 동시에 화면공유하면 각각 별도 기록 생성
- **세션 관리**: `"guildId:userId"` 형태의 키로 서버별 사용자 세션 독립 관리

### 공부 시간 추적 로직
- 화면 공유 시작 시 `StudyLog` 엔티티 생성 및 시작 시간 기록
- 화면 공유 종료 또는 음성 채널 퇴장 시 종료 시간 기록
- 주가 바뀌는 시점 감지하여 기록을 자동 분할 (정확한 주간 통계를 위함)
- 동시성 처리를 위한 `ConcurrentHashMap` 사용
- 각 서버별로 독립적인 세션 관리

### 랭킹 시스템
- 주간 단위로 서버별 공부 시간 집계
- 상위 10위까지 표시
- 개인 통계는 DM으로 프라이버시 보호
- 서버에서 나간 사용자도 기록 유지
- 각 서버의 랭킹은 해당 서버 데이터만 집계

### 다국어 시스템
- **커맨드 매핑**: `CommandConfig`에서 한국어/일본어 명령어를 액션 타입으로 매핑
- **메시지 제공**: `MessageProvider`에서 언어별 메시지 템플릿 관리
- **자동 언어 감지**: 사용한 명령어에 따라 응답 언어 자동 선택
- **시간 포맷**: 언어별로 다른 시간 표시 형식 (예: "1시간 30분" vs "1時間30分")

## 🚨 주의사항

- 봇이 정상 작동하려면 음성 채널 관련 권한이 필요합니다
- 개인 랭킹 조회 시 DM이 차단되어 있으면 메시지를 받을 수 없습니다
- 데이터베이스 연결이 끊어지면 진행 중인 공부 세션이 손실될 수 있습니다
- `주간-랭킹` 채널이 없으면 자동 랭킹 발표가 되지 않습니다

## 📝 로깅

- 애플리케이션 로그는 `logs/study-bot.log` 파일에 저장됩니다
- 일별로 로그 파일이 분할되어 관리됩니다
- 주요 이벤트 (공부 시작/종료, 명령어 실행 등)가 모두 기록됩니다

## 📊 API 예시

### Contribution 히트맵 조회 (GitHub 스타일)
```
GET /api/statistics/contribution/{guildId}/{userId}
```

**응답 예시:**
```json
{
  "userId": "1234567890",
  "userName": "사용자이름",
  "contributions": [
    {
      "date": "2024-12-20",
      "studyTime": 7200,
      "level": 3
    }
  ]
}
```

**레벨 기준:**
- `level 0`: 공부 안함 (회색)
- `level 1`: 0~1시간 (연한 초록)
- `level 2`: 1~2시간 (초록)
- `level 3`: 2~4시간 (진한 초록)
- `level 4`: 4시간 이상 (매우 진한 초록)

### 개인 통계 조회
```
GET /api/statistics/personal/{guildId}/{userId}?period=weekly
```

### 서버 주간 랭킹
```
GET /api/statistics/ranking/{guildId}
```

## 🆕 업데이트 내역

### v2.0.0 (2025-12-20)
- ✨ **멀티 서버 지원**: 여러 Discord 서버에서 독립적으로 작동
- 🌍 **다국어 지원**: 한국어/일본어 이중 커맨드 시스템
- 📈 **웹 API 추가**: 통계, 히트맵, Contribution API 제공
- 🔄 **데이터 마이그레이션**: 기존 데이터 자동 마이그레이션 기능
- 📊 **GitHub 스타일 히트맵**: 일별 공부 기록 시각화 API

### v1.0.0 (Initial Release)
- 기본 공부 시간 추적 기능
- 주간 랭킹 시스템
- Discord 명령어 지원