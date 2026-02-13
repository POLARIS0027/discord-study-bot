package com.studybot.discord_study_bot.pomodoro;

/**
 * 뽀모도로 타이머 상태
 */
public enum PomodoroState {
    STUDY,          // 공부 시간
    SHORT_BREAK,    // 짧은 휴식
    LONG_BREAK,     // 긴 휴식
    PAUSED          // 일시정지
}
