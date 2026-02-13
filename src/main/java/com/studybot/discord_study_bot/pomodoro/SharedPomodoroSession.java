package com.studybot.discord_study_bot.pomodoro;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 공유 뽀모도로 타이머 세션
 */
@Data
public class SharedPomodoroSession {
    // 채널 정보
    private String channelId;           // 음성 채널 ID
    private String guildId;             // 서버 ID
    private String textChannelId;       // 타이머 표시할 텍스트 채널 ID
    private String messageId;           // 타이머 메시지 ID (업데이트용)
    
    // 참여자 관리
    private Set<String> participants;                    // 참여자 userId 목록
    private Map<String, Integer> completedSets;          // 참여자별 완료 세트 수
    private Map<String, Boolean> screenShareStatus;      // 참여자별 화면공유 상태
    
    // 타이머 상태
    private PomodoroState state;        // 현재 상태
    private int studyMinutes;           // 공부 시간 (분)
    private int shortBreakMinutes;      // 짧은 휴식 (분)
    private int longBreakMinutes;       // 긴 휴식 (분)
    private int currentSet;             // 현재 세트 (1~)
    private int totalSets;              // 총 세트 (기본 4)
    private LocalDateTime phaseStartTime; // 현재 단계 시작 시간
    private long remainingSeconds;      // 남은 시간 (초)
    private boolean autoStart;          // 자동 시작 여부
    
    // 스케줄러
    private ScheduledFuture<?> timerTask; // 타이머 스케줄 작업
    
    public SharedPomodoroSession(String channelId, String guildId, String textChannelId) {
        this.channelId = channelId;
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.participants = new HashSet<>();
        this.completedSets = new ConcurrentHashMap<>();
        this.screenShareStatus = new ConcurrentHashMap<>();
        
        // 기본 설정
        this.studyMinutes = 25;
        this.shortBreakMinutes = 5;
        this.longBreakMinutes = 15;
        this.totalSets = 4;
        this.currentSet = 1;
        this.state = PomodoroState.STUDY;
        this.autoStart = false;
    }
    
    /**
     * 참여자 추가
     */
    public void addParticipant(String userId) {
        participants.add(userId);
        completedSets.putIfAbsent(userId, 0);
        screenShareStatus.putIfAbsent(userId, false);
    }
    
    /**
     * 참여자 제거
     */
    public void removeParticipant(String userId) {
        participants.remove(userId);
        completedSets.remove(userId);
        screenShareStatus.remove(userId);
    }
    
    /**
     * 참여자 확인
     */
    public boolean hasParticipant(String userId) {
        return participants.contains(userId);
    }
    
    /**
     * 화면공유 상태 업데이트
     */
    public void updateScreenShareStatus(String userId, boolean isSharing) {
        if (hasParticipant(userId)) {
            screenShareStatus.put(userId, isSharing);
        }
    }
    
    /**
     * 세트 완료 처리
     */
    public void completeSet(String userId) {
        int completed = completedSets.getOrDefault(userId, 0);
        completedSets.put(userId, completed + 1);
    }
    
    /**
     * 남은 시간 1초 감소
     */
    public void decrementSecond() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
        }
    }
    
    /**
     * 다음 단계로 이동
     */
    public void nextPhase() {
        if (state == PomodoroState.STUDY) {
            // 공부 완료 → 휴식
            currentSet++;
            if (currentSet > totalSets) {
                state = PomodoroState.LONG_BREAK;
                remainingSeconds = longBreakMinutes * 60;
            } else {
                state = PomodoroState.SHORT_BREAK;
                remainingSeconds = shortBreakMinutes * 60;
            }
        } else {
            // 휴식 완료 → 공부
            if (state == PomodoroState.LONG_BREAK) {
                // 모든 세트 완료 후 긴 휴식 끝
                currentSet = 1; // 리셋
            }
            state = PomodoroState.STUDY;
            remainingSeconds = studyMinutes * 60;
        }
        phaseStartTime = LocalDateTime.now();
    }
    
    /**
     * 타이머 시작
     */
    public void start() {
        this.remainingSeconds = studyMinutes * 60;
        this.phaseStartTime = LocalDateTime.now();
        this.state = PomodoroState.STUDY;
    }
    
    /**
     * 남은 시간을 "MM:SS" 형식으로 반환
     */
    public String getFormattedRemainingTime() {
        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * 총 시간을 "MM:SS" 형식으로 반환
     */
    public String getFormattedTotalTime() {
        long totalSeconds = switch (state) {
            case STUDY -> studyMinutes * 60;
            case SHORT_BREAK -> shortBreakMinutes * 60;
            case LONG_BREAK -> longBreakMinutes * 60;
            case PAUSED -> remainingSeconds; // 일시정지 상태에서는 남은 시간 사용
        };
        
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
