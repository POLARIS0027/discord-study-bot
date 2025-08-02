package com.studybot.discord_study_bot.repository;

import com.studybot.discord_study_bot.entity.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

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
}
