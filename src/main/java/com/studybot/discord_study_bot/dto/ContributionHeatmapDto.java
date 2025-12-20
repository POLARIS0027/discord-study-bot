package com.studybot.discord_study_bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GitHub 스타일 Contribution 히트맵용 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContributionHeatmapDto {
    private String userId;
    private String userName;
    private List<DailyContribution> contributions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyContribution {
        private String date; // yyyy-MM-dd 형식
        private Long studyTime; // 공부 시간 (초)
        private int level; // 색상 레벨 (0: 없음, 1-4: 공부량에 따라)
    }
}
