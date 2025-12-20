# Discord Study Bot - 인프라 문서

## 📌 프로젝트 개요

### 기본 정보
- **프로젝트명**: Discord Study Bot
- **목적**: Discord 서버에서 학생들의 음성 채널 공부 시간 추적 및 랭킹 제공
- **기술 스택**: Spring Boot 3.5.4, JDA 5.6.1, MySQL 8.0.43, Docker
- **서버**: Oracle Cloud ARM 인스턴스 (4 OCPU, 24GB RAM)

### 주요 기능
- 음성 채널 입장/퇴장 자동 감지
- 공부 시간 자동 기록
- !랭킹 명령어로 순위 조회
- 정기 랭킹 자동 발표

---

## 🏗️ 인프라 아키텍처

### 전체 구조
```
Oracle Cloud ARM Instance (24GB RAM, 4 OCPU)
│
├─ 개발 환경 (Dev) - 독립적 테스트 환경
│  ├─ dev-mysql (1GB, 포트 3307)
│  └─ dev-app (512MB)
│
└─ 운영 환경 (Prod) - Blue-Green 무중단 배포
   ├─ prod-mysql (6GB, 포트 3306) - 공유 DB
   ├─ prod-app-blue (2GB) - 현재 서비스
   └─ prod-app-green (2GB) - 다음 배포 대기
```

### 리소스 분배
| 환경 | 컴포넌트 | 메모리 | CPU | 상태 |
|------|----------|--------|-----|------|
| Dev | MySQL | 1GB | 1.0 | 필요시만 |
| Dev | App | 512MB | 0.5 | 필요시만 |
| Prod | MySQL | 6GB | 2.0 | 항상 실행 |
| Prod | Blue | 2GB | 2.0 | 활성 서비스 |
| Prod | Green | 2GB | 2.0 | 배포 시에만 |
| - | 여유 | ~12GB | - | 안전 마진 |

### 네트워크 구조
```
study-bot-dev-network (개발 전용)
├─ dev-mysql:3307
└─ dev-app

study-bot-prod-network (운영 전용)
├─ study-bot-prod-mysql:3306
├─ study-bot-prod-app-blue
└─ study-bot-prod-app-green
```

---

## 📁 디렉토리 구조

### 서버 (ARM 인스턴스)
```
/home/ubuntu/discord-study-bot/
│
├── dev/                              # 개발 환경
│   ├── docker-compose.yaml           # Dev DB + App 통합
│   ├── Dockerfile                    # Dev 앱 이미지
│   ├── custom-my.cnf                 # MySQL 개발 설정 (512MB)
│   ├── .env                          # 개발용 환경 변수
│   ├── build/libs/*.jar              # 개발용 JAR
│   └── logs/                         # 개발 로그
│
├── prod/                             # 운영 환경
│   ├── db/                           # 운영 DB (공유)
│   │   ├── docker-compose.yaml
│   │   ├── custom-my.cnf             # MySQL 운영 설정 (4GB)
│   │   └── .env
│   │
│   ├── blue/                         # Blue 슬롯
│   │   ├── docker-compose.yaml
│   │   ├── Dockerfile
│   │   ├── .env
│   │   └── build/libs/*.jar
│   │
│   ├── green/                        # Green 슬롯
│   │   ├── docker-compose.yaml
│   │   ├── Dockerfile
│   │   ├── .env
│   │   └── build/libs/*.jar
│   │
│   ├── current -> blue               # 심볼릭 링크 (현재 활성)
│   └── logs/                         # 운영 로그 (Blue/Green 공유)
│
├── scripts/                          # 관리 스크립트
│   ├── dev-start.sh                  # 개발 환경 시작
│   ├── dev-stop.sh                   # 개발 환경 중지
│   ├── dev-rebuild.sh                # 개발 환경 재빌드
│   ├── prod-deploy.sh                # 운영 배포 준비
│   ├── prod-switch.sh                # Blue ↔ Green 전환
│   ├── prod-rollback.sh              # 운영 롤백
│   └── status.sh                     # 전체 상태 확인
│
└── shared/                           # 공유 리소스
    └── logs/                         # 공유 로그
```

### 로컬 (개발 환경)
```
E:\github\studybot\discord-study-bot/
│
├── src/                              # 소스 코드
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── application-prod.yml
│   └── test/
│
├── build.gradle                      # Gradle 설정
├── gradlew, gradlew.bat              # Gradle Wrapper
│
├── dev/                              # 개발 환경 설정 (서버에 업로드)
├── prod/                             # 운영 환경 설정 (서버에 업로드)
├── scripts/                          # 관리 스크립트 (서버에 업로드)
├── .dockerignore
└── README-deployment.md
```

---

## ⚙️ 주요 설정 파일

### 1. prod/db/docker-compose.yaml
**용도**: 운영 MySQL 설정
```yaml
# 핵심 설정
- image: mysql:8.0.43
- memory: 6GB
- port: 3306 (127.0.0.1만 접근)
- volume: study-bot-prod-mysql-data (영구 저장)
- network: study-bot-prod-network
```

### 2. prod/db/custom-my.cnf
**용도**: MySQL 성능 최적화
```ini
# 주요 설정
innodb_buffer_pool_size = 4G      # 64MB → 4GB (62배 향상)
max_connections = 200
table_open_cache = 2000
```

### 3. prod/blue/docker-compose.yaml
**용도**: Blue 앱 컨테이너 설정
```yaml
# 핵심 설정
- image: discord-study-bot:prod-blue
- memory: 2GB
- restart: "no" (수동 관리)
- network: study-bot-prod-network (DB와 공유)
- datasource: jdbc:mysql://study-bot-prod-mysql:3306/...
```

### 4. prod/blue/Dockerfile
**용도**: 앱 컨테이너 이미지
```dockerfile
FROM eclipse-temurin:17-jre-jammy  # ARM64 호환 Java 17
COPY build/libs/*.jar app.jar      # 로컬 빌드 JAR
ENV JAVA_OPTS="-Xms512m -Xmx1536m"
```

### 5. .env 파일 (각 환경별)
**용도**: 환경 변수 (Git에서 제외)
```bash
# prod/db/.env
MYSQL_ROOT_PASSWORD=lize123
MYSQL_PASSWORD=lize123

# prod/blue/.env, prod/green/.env
MYSQL_PASSWORD=lize123
DISCORD_TOKEN=<실제_토큰>

# dev/.env
MYSQL_ROOT_PASSWORD=dev_password
MYSQL_PASSWORD=dev_password
DEV_DISCORD_TOKEN=<개발용_토큰>
```

---

## 🚀 배포 프로세스

### 개발 환경 사용

#### 1. 개발 환경 시작
```bash
ssh ubuntu@138.2.59.61
cd ~/discord-study-bot
./scripts/dev-start.sh
```

#### 2. 새 코드 테스트
```bash
# 로컬에서 빌드
./gradlew clean build -x test

# 서버에 업로드
scp -i <ssh-key> build/libs/*.jar ubuntu@138.2.59.61:~/discord-study-bot/dev/build/libs/

# 재빌드 및 재시작
ssh ubuntu@138.2.59.61
./scripts/dev-rebuild.sh
```

#### 3. 개발 환경 중지
```bash
./scripts/dev-stop.sh
```

### 운영 배포 (Blue-Green)

#### 1. 로컬에서 빌드
```bash
cd E:\github\studybot\discord-study-bot
./gradlew clean build -x test
```

#### 2. 서버에 업로드
```bash
# 현재 활성이 Blue라면 Green에 업로드
scp -i <ssh-key> build/libs/*.jar ubuntu@138.2.59.61:~/discord-study-bot/prod/green/build/libs/
```

#### 3. 배포 준비
```bash
ssh ubuntu@138.2.59.61
cd ~/discord-study-bot
./scripts/prod-deploy.sh
```
- 자동으로 Green(또는 Blue) 이미지 빌드
- 아직 시작하지 않음 (테스트 가능)

#### 4. Blue-Green 전환
```bash
./scripts/prod-switch.sh
```
**작동 방식**:
1. 현재 활성(Blue) 중지
2. 새 버전(Green) 시작
3. 헬스체크 (30초)
4. `current` 심볼릭 링크 변경
5. 완료

**다운타임**: 약 15초

#### 5. 검증
```bash
./scripts/status.sh
```
- Discord에서 봇 온라인 확인
- !랭킹 명령어 테스트
- 음성 채널 입장/퇴장 테스트

#### 6. 롤백 (문제 발생 시)
```bash
./scripts/prod-rollback.sh
```
- 즉시 이전 버전으로 복귀
- 15초 내 완료

---

## 📊 스크립트 상세 설명

### dev-start.sh
```bash
# 용도: 개발 환경 시작
# 위치: ~/discord-study-bot/scripts/
# 실행: ./scripts/dev-start.sh

# 동작:
1. dev/ 디렉토리로 이동
2. docker compose up -d (DB + 앱 동시 시작)
3. 로그 출력
```

### dev-rebuild.sh
```bash
# 용도: 새 JAR로 개발 환경 재빌드
# 실행: ./scripts/dev-rebuild.sh

# 동작:
1. docker compose down (전체 중지)
2. docker compose build --no-cache (이미지 재빌드)
3. docker compose up -d (재시작)
```

### prod-deploy.sh
```bash
# 용도: 운영 배포 준비 (빌드만)
# 실행: ./scripts/prod-deploy.sh

# 동작:
1. 현재 활성 확인 (Blue 또는 Green)
2. 반대쪽을 배포 대상으로 선택
3. JAR 파일 존재 확인
4. Docker 이미지 빌드
5. 전환 안내 메시지 출력
```

### prod-switch.sh
```bash
# 용도: Blue ↔ Green 전환 (무중단 배포)
# 실행: ./scripts/prod-switch.sh

# 동작:
1. 현재 활성 중지 (docker compose stop)
2. 대기 슬롯 시작 (docker compose up -d)
3. 30초 대기 (Discord 연결 시간)
4. 헬스체크 (컨테이너 Up 확인)
5. current 심볼릭 링크 변경
6. 완료 메시지
```

### prod-rollback.sh
```bash
# 용도: 긴급 롤백 (이전 버전 복구)
# 실행: ./scripts/prod-rollback.sh

# 동작:
1. 현재 버전 중지
2. 이전 버전 시작
3. current 링크 복원
4. 완료
```

### status.sh
```bash
# 용도: 전체 시스템 상태 확인
# 실행: ./scripts/status.sh

# 출력:
- 개발 환경 상태
- 운영 DB 상태
- Blue/Green 상태
- 현재 활성 환경
- 리소스 사용량
```

---

## 🔧 일상 운영 명령어

### 상태 확인
```bash
# 전체 상태
./scripts/status.sh

# 특정 환경 로그
docker compose -f ~/discord-study-bot/prod/blue/docker-compose.yaml logs -f

# 실시간 리소스 모니터링
docker stats

# DB 접속
docker exec -it study-bot-prod-mysql mysql -u root -plize123 study_bot_db
```

### 수동 제어
```bash
# 운영 DB 재시작
cd ~/discord-study-bot/prod/db
docker compose restart

# Blue 앱 재시작
cd ~/discord-study-bot/prod/blue
docker compose restart

# 로그 확인
tail -f ~/discord-study-bot/prod/logs/study-bot.log
```

### 데이터 백업
```bash
# DB 백업 (정기적으로 실행)
docker exec study-bot-prod-mysql mysqldump \
  -u root -plize123 \
  --all-databases \
  --single-transaction \
  > ~/backup-$(date +%Y%m%d).sql

# 로컬로 다운로드
scp ubuntu@138.2.59.61:~/backup-*.sql C:\backup\
```

---

## 🔒 보안 고려사항

### 환경 변수 관리
- `.env` 파일은 절대 Git에 커밋하지 않음
- `.gitignore`에 `.env` 포함
- Discord 토큰은 환경 변수로만 관리

### 네트워크 보안
- MySQL 포트는 `127.0.0.1`만 접근 허용
- 컨테이너 간 통신은 Docker 내부 네트워크 사용
- 외부 노출 최소화

### 백업 전략
- 매주 전체 DB 백업
- 백업 파일은 로컬 + 클라우드 이중 저장
- 복원 테스트 정기적 실행

---

## 🐛 트러블슈팅

### 봇이 오프라인
```bash
# 1. 컨테이너 상태 확인
docker compose -f ~/discord-study-bot/prod/current/docker-compose.yaml ps

# 2. 로그 확인
docker compose -f ~/discord-study-bot/prod/current/docker-compose.yaml logs

# 3. Discord 토큰 확인
docker exec study-bot-prod-app-blue env | grep DISCORD_TOKEN

# 4. 재시작
docker compose -f ~/discord-study-bot/prod/current/docker-compose.yaml restart
```

### DB 연결 실패
```bash
# 1. DB 상태 확인
docker compose -f ~/discord-study-bot/prod/db/docker-compose.yaml ps

# 2. 네트워크 확인
docker network inspect study-bot-prod-network

# 3. MySQL 접속 테스트
docker exec study-bot-prod-mysql mysql -u root -plize123 -e "SELECT 1;"

# 4. 앱에서 DB로 ping
docker exec study-bot-prod-app-blue ping -c 3 study-bot-prod-mysql
```

### 메모리 부족
```bash
# 리소스 사용량 확인
docker stats

# 개발 환경 중지
./scripts/dev-stop.sh

# 불필요한 컨테이너/이미지 정리
docker system prune -a
```

### 배포 실패
```bash
# 즉시 롤백
./scripts/prod-rollback.sh

# 로그 확인
docker compose logs

# 문제 해결 후 재배포
./scripts/prod-deploy.sh
./scripts/prod-switch.sh
```

---

## 📚 마이그레이션 기록

### 기존 구조 (2개 인스턴스)
```
인스턴스 1: MySQL (1GB RAM, AMD)
인스턴스 2: Java 앱 직접 실행 (1GB RAM, AMD)
```

### 마이그레이션 과정 (2025-12-06)
1. **백업** (기존 DB 서버)
   - Volume 백업: `db-volume-backup-final-*.tar.gz`
   - mysqldump 백업: `db-dump-backup-final-*.sql` (3.67MB)

2. **ARM 인스턴스 생성**
   - Shape: VM.Standard.A1.Flex
   - OCPU: 4
   - RAM: 24GB
   - Boot Volume: 200GB

3. **환경 구축**
   - Docker 설치
   - 디렉토리 구조 생성
   - 설정 파일 업로드
   - mysqldump로 데이터 복원

4. **결과**
   - 성능 향상: MySQL 버퍼 64MB → 4GB (62배)
   - Blue-Green 배포 구조 확립
   - 개발/운영 환경 분리

### 신규 구조 (1개 인스턴스)
```
ARM 인스턴스 1개 (24GB RAM)
├─ Dev: MySQL + App
└─ Prod: MySQL + Blue/Green Apps
```

---

## 📖 참고 자료

### 접속 정보
```bash
# SSH 접속
ssh -i C:\Users\SJW-DESKTOP\.ssh\ubuntu\ssh-key-2025-12-06.key ubuntu@138.2.59.61

# 작업 디렉토리
cd ~/discord-study-bot
```

### Docker 네트워크
- Dev: `study-bot-dev-network`
- Prod: `study-bot-prod-network`

### Docker 볼륨
- Dev: `study-bot-dev-mysql-data`
- Prod: `study-bot-prod-mysql-data`

### 환경 변수
- `SPRING_PROFILES_ACTIVE`: local (개발) / prod (운영)
- `MYSQL_PASSWORD`: lize123
- `DISCORD_TOKEN`: (실제 토큰)

---

## 🎯 추후 개선 사항

### 단기
- [ ] Nginx 리버스 프록시 추가 (웹 페이지 제공용)
- [ ] Let's Encrypt SSL 인증서
- [ ] 자동 백업 cron 설정
- [ ] Portainer (Docker UI) 설치

### 중기
- [ ] 학생용 웹 페이지 개발 (공부 시간 조회)
- [ ] REST API 추가
- [ ] Prometheus + Grafana 모니터링

### 장기
- [ ] CI/CD 파이프라인 (GitHub Actions)
- [ ] Redis 캐시 레이어
- [ ] 로그 수집 시스템 (ELK Stack)

---

## 📞 연락처 및 이슈 트래킹

- **GitHub**: [Repository URL]
- **담당자**: [이름]
- **마지막 업데이트**: 2025-12-06

---

**문서 버전**: 1.0
**작성일**: 2025-12-06
**다음 검토일**: 2026-01-06

