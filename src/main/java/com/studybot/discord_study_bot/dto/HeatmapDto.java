package com.studybot.discord_study_bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDto {
    private String userId;
    private String userName;
    private List<HeatmapCell> heatmapData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapCell {
        private int hour; // 0-23
        private int dayOfWeek; // 1(월) - 7(일)
        private long count; // 해당 시간대의 공부 세션 수
    }
}
