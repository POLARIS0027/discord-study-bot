package com.studybot.discord_study_bot.service;

import com.studybot.discord_study_bot.entity.StudyLog;
import com.studybot.discord_study_bot.repository.StudyLogRepository;
import com.studybot.discord_study_bot.session.StudySession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 화면공유와 뽀모도로를 통합 관리하는 세션 매니저
 * 둘 중 하나라도 활성이면 StudyLog를 유지
 */
@Service
@RequiredArgsConstructor
public class StudySessionManager {

    private static final Logger logger = LoggerFactory.getLogger(StudySessionManager.class);
    private final StudyLogRepository studyLogRepository;

    // Key: "guildId:userId", Value: StudySession
    private final Map<String, StudySession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 세션 조회 또는 생성
     */
    public StudySession getOrCreateSession(String guildId, String userId) {
        String sessionKey = guildId + ":" + userId;
        return activeSessions.computeIfAbsent(sessionKey, k -> new StudySession(guildId, userId));
    }

    /**
     * 세션 조회
     */
    public StudySession getSession(String guildId, String userId) {
        String sessionKey = guildId + ":" + userId;
        return activeSessions.get(sessionKey);
    }

    /**
     * 화면공유 시작
     */
    public void startScreenShare(String guildId, String guildName, String userId, String userName) {
        StudySession session = getOrCreateSession(guildId, userId);
        session.setScreenSharing(true);
        ensureStudyLogActive(session, guildId, guildName, userId, userName);
        
        logger.info("[{}] {}님이 화면공유를 시작했습니다. StudyLog 활성화.", guildName, userName);
    }

    /**
     * 화면공유 종료
     */
    public void stopScreenShare(String guildId, String userId, String userName) {
        StudySession session = getSession(guildId, userId);
        if (session != null) {
            session.setScreenSharing(false);
            checkAndCloseStudyLog(session, userName);
        }
    }

    /**
     * 뽀모도로 시작 (공부 단계)
     */
    public void startPomodoroStudy(String guildId, String guildName, String userId, String userName) {
        StudySession session = getOrCreateSession(guildId, userId);
        session.setPomodoroActive(true);
        ensureStudyLogActive(session, guildId, guildName, userId, userName);
        
        logger.info("[{}] {}님이 뽀모도로 공부를 시작했습니다. StudyLog 활성화.", guildName, userName);
    }

    /**
     * 뽀모도로 일시정지 (휴식 단계)
     */
    public void pausePomodoro(String guildId, String userId, String userName) {
        StudySession session = getSession(guildId, userId);
        if (session != null) {
            session.setPomodoroActive(false);
            checkAndCloseStudyLog(session, userName);
        }
    }

    /**
     * 강제 세션 종료 (음성 채널 퇴장 시)
     */
    public void forceCloseSession(String guildId, String userId, String userName) {
        StudySession session = getSession(guildId, userId);
        if (session != null) {
            // 모든 활동 종료
            session.setScreenSharing(false);
            session.setPomodoroActive(false);
            
            // StudyLog 종료
            closeStudyLog(session, userName);
            
            // 세션 제거
            activeSessions.remove(session.getSessionKey());
            
            logger.info("{}님의 세션이 강제 종료되었습니다. (음성 채널 퇴장)", userName);
        }
    }

    /**
     * StudyLog 활성 상태 보장
     * 화면공유 또는 뽀모도로가 활성이면 StudyLog 생성/유지
     */
    private void ensureStudyLogActive(StudySession session, String guildId, String guildName, 
                                      String userId, String userName) {
        if (session.getStudyLogId() == null) {
            // StudyLog 생성
            StudyLog log = new StudyLog();
            log.setGuildId(guildId);
            log.setGuildName(guildName);
            log.setUserId(userId);
            log.setUserName(userName);
            log.setStartTime(LocalDateTime.now());
            
            StudyLog saved = studyLogRepository.save(log);
            session.setStudyLogId(saved.getId());
            session.setStartTime(LocalDateTime.now());
            
            logger.info("StudyLog 생성 완료. ID: {}", saved.getId());
        }
    }

    /**
     * StudyLog 종료 확인 및 처리
     * 화면공유와 뽀모도로가 모두 비활성이면 StudyLog 종료
     */
    private void checkAndCloseStudyLog(StudySession session, String userName) {
        if (!session.isActive()) {
            closeStudyLog(session, userName);
        } else {
            logger.info("{}님의 세션이 여전히 활성 상태입니다. (화면공유: {}, 뽀모도로: {})", 
                userName, session.isScreenSharing(), session.isPomodoroActive());
        }
    }

    /**
     * StudyLog 종료
     */
    private void closeStudyLog(StudySession session, String userName) {
        if (session.getStudyLogId() != null) {
            StudyLog log = studyLogRepository.findById(session.getStudyLogId()).orElse(null);
            if (log != null && log.getEndTime() == null) {
                log.setEndTime(LocalDateTime.now());
                studyLogRepository.save(log);
                
                logger.info("{}님의 StudyLog 종료 완료. ID: {}, 시작: {}, 종료: {}", 
                    userName, log.getId(), log.getStartTime(), log.getEndTime());
            }
            session.setStudyLogId(null);
        }
    }

    /**
     * 활성 세션 맵 조회 (VoiceChannelListener 호환용)
     */
    public Map<String, StudySession> getActiveSessions() {
        return activeSessions;
    }
}
