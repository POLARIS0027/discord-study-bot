package com.studybot.discord_study_bot.repository;

import com.studybot.discord_study_bot.entity.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository // 스프링에게 데이터 리포지토리라고 알림
public interface StudyLogRepository extends JpaRepository<StudyLog, Long> { // 툴킷이 관리할 대상의 타입과 id의 타입을 알림

        /**
         * 특정 서버의 특정 사용자의 가장 최근에 시작하고 아직 끝나지 않은 공부 기록을 찾음.
         * 
         * @param guildId 서버의 Discord Guild ID
         * @param userId  사용자의 디스코드 ID
         * @return StudyLog Optional 객체
         */
        @Query("SELECT s FROM StudyLog s WHERE s.guildId = :guildId AND s.userId = :userId AND s.endTime IS NULL ORDER BY s.startTime DESC LIMIT 1")
        Optional<StudyLog> findLatestUnfinishedLogByGuildAndUser(@Param("guildId") String guildId,
                        @Param("userId") String userId);

        /**
         * 특정 서버의 기간 내 사용자의 공부 시간 합계를 계산하여 랭킹을 반환.
         *
         * @param guildId       서버의 Discord Guild ID
         * @param startDate     시작일
         * @param endDate       종료일
         * @param excludeUserId 제외할 userID
         * @return 사용자 이름과 공부 시간(초)의 합계를 포함한 리스트
         */
        @Query(value = "SELECT s.user_id, SUM(TIMESTAMPDIFF(SECOND, s.start_time, s.end_time)) " +
                        "FROM study_log s " +
                        "WHERE s.guild_id = :guildId AND s.start_time >= :startDate AND s.end_time <= :endDate " +
                        "AND s.end_time IS NOT NULL AND s.user_id != :excludeUserId " +
                        "GROUP BY s.user_id " +
                        "ORDER BY SUM(TIMESTAMPDIFF(SECOND, s.start_time, s.end_time)) DESC " +
                        "LIMIT 10", nativeQuery = true)
        List<Object[]> findRankingsByPeriodAndGuild(@Param("guildId") String guildId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("excludeUserId") String excludeUserId);

        /**
         * 특정 서버의 이번 주 개인 공부 시간 조회
         * 
         * @param guildId   서버의 Discord Guild ID
         * @param userId    유저 고유 ID
         * @param startDate 시작일
         * @param endDate   종료일
         */
        @Query(value = "SELECT SUM(TIMESTAMPDIFF(SECOND, s.start_time, s.end_time)) " +
                        "FROM study_log s " +
                        "WHERE s.guild_id = :guildId AND s.user_id = :userId " +
                        "AND s.start_time >= :startDate AND s.end_time <= :endDate AND s.end_time IS NOT NULL", nativeQuery = true)
        Optional<Long> findTotalDurationByUserIdPeriodAndGuild(@Param("guildId") String guildId,
                        @Param("userId") String userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // ===== 웹 통계용 쿼리 메서드 =====

        /**
         * 일별 공부 시간 조회 (히트맵/개인 통계용)
         * 
         * @param guildId 서버의 Discord Guild ID
         * @param userId  유저 고유 ID
         * @param start   시작일
         * @param end     종료일
         * @return [날짜, 공부시간(초)] 배열의 리스트
         */
        @Query(value = "SELECT DATE(s.start_time), SUM(TIMESTAMPDIFF(SECOND, s.start_time, s.end_time)) " +
                        "FROM study_log s " +
                        "WHERE s.guild_id = :guildId AND s.user_id = :userId " +
                        "AND s.start_time BETWEEN :start AND :end AND s.end_time IS NOT NULL " +
                        "GROUP BY DATE(s.start_time) " +
                        "ORDER BY DATE(s.start_time)", nativeQuery = true)
        List<Object[]> findDailyStudyTime(@Param("guildId") String guildId,
                        @Param("userId") String userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * 시간대별 공부 패턴 (히트맵용)
         * 
         * @param guildId 서버의 Discord Guild ID
         * @param userId  유저 고유 ID
         * @return [시간(0-23), 요일(1-7), 횟수] 배열의 리스트
         */
        @Query(value = "SELECT HOUR(s.start_time), DAYOFWEEK(s.start_time), COUNT(*) " +
                        "FROM study_log s " +
                        "WHERE s.guild_id = :guildId AND s.user_id = :userId AND s.end_time IS NOT NULL " +
                        "GROUP BY HOUR(s.start_time), DAYOFWEEK(s.start_time) " +
                        "ORDER BY DAYOFWEEK(s.start_time), HOUR(s.start_time)", nativeQuery = true)
        List<Object[]> findStudyPatternHeatmap(@Param("guildId") String guildId,
                        @Param("userId") String userId);

        /**
         * 연속 기록 조회 (Streak용)
         * 
         * @param guildId 서버의 Discord Guild ID
         * @param userId  유저 고유 ID
         * @param start   시작일
         * @return 공부한 날짜 리스트
         */
        @Query(value = "SELECT DISTINCT DATE(s.start_time) " +
                        "FROM study_log s " +
                        "WHERE s.guild_id = :guildId AND s.user_id = :userId " +
                        "AND s.start_time >= :start AND s.end_time IS NOT NULL " +
                        "ORDER BY DATE(s.start_time)", nativeQuery = true)
        List<String> findStudyDates(@Param("guildId") String guildId,
                        @Param("userId") String userId,
                        @Param("start") LocalDateTime start);

        /**
         * 특정 서버의 특정 사용자의 가장 최근 레코드 조회 (사용자명/길드명 획득용)
         * 
         * @param guildId 서버의 Discord Guild ID
         * @param userId  유저 고유 ID
         * @return 가장 최근 StudyLog Optional 객체
         */
        Optional<StudyLog> findTopByGuildIdAndUserIdOrderByIdDesc(String guildId, String userId);
}
