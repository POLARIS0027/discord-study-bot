package com.studybot.discord_study_bot.session;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 사용자의 통합 공부 세션 정보
 * 화면공유와 뽀모도로를 통합 관리
 */
@Data
public class StudySession {
    private String sessionKey;           // "guildId:userId"
    private String guildId;
    private String userId;
    private Long studyLogId;             // 현재 활성 StudyLog ID
    private boolean isScreenSharing;     // 화면공유 여부
    private boolean isPomodoroActive;    // 뽀모도로 활성 여부
    private LocalDateTime startTime;     // 세션 시작 시간
    
    public StudySession(String guildId, String userId) {
        this.guildId = guildId;
        this.userId = userId;
        this.sessionKey = guildId + ":" + userId;
        this.isScreenSharing = false;
        this.isPomodoroActive = false;
    }
    
    /**
     * 세션이 활성 상태인지 확인
     * 화면공유 또는 뽀모도로 중 하나라도 활성이면 true
     */
    public boolean isActive() {
        return isScreenSharing || isPomodoroActive;
    }
}
