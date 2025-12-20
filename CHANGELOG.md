# Changelog

All notable changes to Discord Study Bot will be documented in this file.

## [2.0.0] - 2025-12-20

### ✨ Added

#### 멀티 서버 지원
- 여러 Discord 서버에서 동시에 작동 가능
- 서버별 독립적인 데이터 및 랭킹 시스템
- `StudyLog` 엔티티에 `guildId`, `guildName` 컬럼 추가
- 동일 사용자의 여러 서버 동시 활동 지원 (`guildId:userId` 세션 키)

#### 다국어 지원 (한국어/일본어)
- 이중 커맨드 시스템 구현
  - 한국어: `!도움말`, `!주간랭킹`, `!내랭킹`, `!이벤트`, `!리제`
  - 日本語: `!ヘルプ`, `!週間ランキング`, `!マイランキング`, `!イベント`, `!リゼ`
- `CommandConfig`: 커맨드 매핑 관리
- `MessageProvider`: 다국어 메시지 템플릿 관리
- 명령어에 따른 자동 언어 감지 및 응답

#### 웹 통계 API
- **REST API 엔드포인트**:
  - `GET /api/statistics/personal/{guildId}/{userId}` - 개인 통계 (일별/주별/월별)
  - `GET /api/statistics/ranking/{guildId}` - 서버 주간 랭킹
  - `GET /api/statistics/contribution/{guildId}/{userId}` - GitHub 스타일 Contribution 히트맵
  - `GET /api/statistics/heatmap/{guildId}/{userId}` - 시간대별 공부 패턴
  - `GET /api/statistics/streak/{guildId}/{userId}` - 연속 공부 일수
  - `GET /api/statistics/event-ranking/{guildId}` - 이벤트 기간 랭킹

- **새로운 DTO**:
  - `ContributionHeatmapDto`: GitHub 스타일 일별 공부 기록
    - 최근 1년간 데이터
    - 공부 시간에 따른 5단계 레벨 (0~4)
  - `PersonalStatsDto`: 개인 통계 정보
  - `HeatmapDto`: 시간대/요일별 패턴
  - `StreakDto`: 연속 기록 정보

- **서비스 계층**:
  - `StatisticsService`: 통계 데이터 처리 비즈니스 로직
  - `StatisticsController`: REST API 엔드포인트

#### 데이터 마이그레이션
- `DataMigrationService`: 기존 데이터 자동 마이그레이션
- 애플리케이션 시작 시 `guild_id`가 NULL인 레코드 자동 업데이트
- 설정 기반 마이그레이션 (`migration.legacy-guild-id`, `migration.legacy-guild-name`)

### 🔧 Changed

#### 데이터베이스 스키마
- `study_log` 테이블:
  - `guild_id` VARCHAR(255) 추가
  - `guild_name` VARCHAR(255) 추가
  - `created_at` DATETIME 추가 (레코드 생성 시각)
  - 모든 컬럼 `utf8mb4_unicode_ci` 인코딩

#### Repository 쿼리
- 모든 쿼리에 `guildId` 필터 조건 추가
- 메서드명 변경:
  - `findLatestUnfinishedLogByUserId` → `findLatestUnfinishedLogByGuildAndUser`
  - `findRankingsByPeriod` → `findRankingsByPeriodAndGuild`
  - `findTotalDurationByUserIdAndPeriod` → `findTotalDurationByUserIdPeriodAndGuild`
- 새 쿼리 메서드:
  - `findDailyStudyTime`: 일별 공부 시간 조회
  - `findStudyPatternHeatmap`: 시간대별 패턴
  - `findStudyDates`: 연속 기록용 날짜 목록
  - `findTopByGuildIdAndUserIdOrderByIdDesc`: 최근 레코드 조회

#### Service 계층
- `RankingService`: 모든 메서드에 `guildId` 파라미터 추가
  - `getWeeklyRanking(guildId)`
  - `getPreviousWeeklyRanking(guildId)`
  - `getWeeklyTotalStudyTimeForUser(guildId, userId)`
  - `getEventRanking(guildId)`
  - `getEventTotalStudyTimeForUser(guildId, userId)`

#### Listener
- `VoiceChannelListener`: 
  - 이벤트에서 `guildId`, `guildName` 추출
  - 세션 키를 `"guildId:userId"` 형태로 변경
  - 멀티 서버 동시 활동 지원
- `RankingCommandListener`:
  - 이중 커맨드 구조로 완전 리팩토링
  - `CommandConfig`와 `MessageProvider` 통합
  - 모든 메서드에 `guildId` 전달

#### Scheduler
- `RankingScheduler`: 
  - 모든 길드를 순회하며 각 서버의 `주간-랭킹` 채널에 독립적으로 포스팅
  - 서버별 랭킹 조회 로직 적용

### 🐛 Fixed
- UTF-8 인코딩 문제 해결 (MySQL `utf8mb4` 사용)
- 멀티 서버 환경에서 세션 충돌 방지
- 사용자명/길드명 NULL 처리

### 📚 Documentation
- `README.md` 업데이트
  - 멀티 서버 지원 설명 추가
  - 다국어 명령어 목록 추가
  - API 예시 추가
  - 업데이트 내역 섹션 추가
- `CHANGELOG.md` 생성
- API 엔드포인트 문서화

### 🔒 Security
- 프로파일별 설정 분리 (`application-local.yml`, `application-prod.yml`)
- 민감한 정보 환경 변수화 (`DISCORD_TOKEN`, `MYSQL_PASSWORD`, etc.)

### ⚙️ Configuration
- Spring Profiles 지원 (`local`, `prod`)
- 프로파일별 데이터베이스 설정
- 프로파일별 로깅 레벨 설정
- Docker Compose 환경 변수 지원

## [1.0.0] - Initial Release

### Added
- 기본 공부 시간 추적 기능
- 음성 채널 화면 공유 감지
- 주간 랭킹 시스템
- 개인 통계 DM 전송
- 자동 주간 랭킹 발표 스케줄러
- MySQL 데이터베이스 연동
- Docker Compose 지원
