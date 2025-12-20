package com.studybot.discord_study_bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);
    private final JdbcTemplate jdbcTemplate;

    @Value("${migration.legacy-guild-id:}")
    private String legacyGuildId;

    @Value("${migration.legacy-guild-name:}")
    private String legacyGuildName;

    @PostConstruct
    public void migrateExistingData() {
        // legacy guild ID가 설정되어 있지 않으면 마이그레이션 스킵
        if (legacyGuildId == null || legacyGuildId.isEmpty()) {
            logger.info("Legacy guild ID가 설정되지 않아 데이터 마이그레이션을 스킵합니다.");
            return;
        }

        try {
            // guild_id가 NULL인 레코드 수 확인
            Integer nullCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM study_log WHERE guild_id IS NULL",
                    Integer.class);

            if (nullCount != null && nullCount > 0) {
                logger.info("{}개의 NULL guild_id 레코드를 발견했습니다. 마이그레이션을 시작합니다.", nullCount);

                // NULL인 레코드 업데이트
                int updated = jdbcTemplate.update(
                        "UPDATE study_log SET guild_id = ?, guild_name = ? WHERE guild_id IS NULL",
                        legacyGuildId,
                        legacyGuildName);

                logger.info("데이터 마이그레이션 완료: {}개의 레코드가 업데이트되었습니다.", updated);
            } else {
                logger.info("마이그레이션이 필요한 데이터가 없습니다.");
            }
        } catch (Exception e) {
            logger.error("데이터 마이그레이션 중 오류 발생", e);
        }
    }
}
