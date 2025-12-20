package com.studybot.discord_study_bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalStatsDto {
    private String userId;
    private String userName;
    private String guildId;
    private String guildName;
    private Long totalStudyTime; // 총 공부 시간 (초)
    private List<DailyStudyDto> dailyStats; // 일별 공부 시간

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStudyDto {
        private String date; // yyyy-MM-dd 형식
        private Long studyTime; // 해당 날짜의 공부 시간 (초)
    }
}
