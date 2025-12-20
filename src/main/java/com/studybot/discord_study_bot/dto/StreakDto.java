package com.studybot.discord_study_bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreakDto {
    private String userId;
    private String userName;
    private int currentStreak; // 현재 연속 공부 일수
    private int longestStreak; // 최장 연속 공부 일수
    private List<String> studyDates; // 공부한 날짜 목록 (yyyy-MM-dd)
}
