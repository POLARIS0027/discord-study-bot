package com.studybot.discord_study_bot.repository;

import com.studybot.discord_study_bot.entity.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List; //

import java.util.Optional;



@Repository // 스프리에게 데이터 리포지토리라고 알림
public interface StudyLogRepository extends JpaRepository<StudyLog, Long>{ // 툴킷이 관리할 대상의 타입과 id의 타입을 알림

    /**
     * 특정 사용자의 가장 최근에 시작하고 아직 끝나지 않은 공부 기록을 찾음.
     * @param userId 사용자의 디스코드 ID
     * @return StudyLog Optional 객체
     */
    @Query("SELECT s FROM StudyLog s WHERE s.userId = :userId AND s.endTime IS NULL ORDER BY s.startTime DESC LIMIT 1")
    Optional<StudyLog> findLatestUnfinishedLogByUserId(@Param("userId") String userId);

    /**
     * 기간 내 사용자의 공부 시간 합계를 계산하여 랭킹을 반환.
     *
     * @param startDate     시작일
     * @param endDate       종료일
     * @param excludeUserId 제외할 userID
     * @return 사용자 이름과 공부 시간(초)의 합계를 포함한 리스트
     */
    // 리턴 타입을 List<Object[]>를 리턴함 !! Todo:MySQL 쿼리를 JPQL쿼리 사용하도록 수정해야 함. 임시조치임
    @Query(value = "SELECT s.user_id, SUM(TIMESTAMPDIFF(SECOND, s.start_time, s.end_time)) " +
            "FROM study_log s " +
            "WHERE s.start_time >= :startDate AND s.end_time <= :endDate AND s.end_time IS NOT NULL " +
            "AND s.user_id != :excludeUserId " +
            "GROUP BY s.user_id " +
            "ORDER BY SUM(TIMESTAMPDIFF(SECOND, s.start_time, s.end_time)) DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<Object[]> findRankingsByPeriod(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        @Param("excludeUserId") String excludeUserId);
}
