package com.studybot.discord_study_bot.controller;

import com.studybot.discord_study_bot.dto.ContributionHeatmapDto;
import com.studybot.discord_study_bot.dto.HeatmapDto;
import com.studybot.discord_study_bot.dto.PersonalStatsDto;
import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.dto.StreakDto;
import com.studybot.discord_study_bot.service.RankingService;
import com.studybot.discord_study_bot.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 웹 통계를 위한 REST API 컨트롤러
 * 향후 웹 프론트엔드와 연동 시 사용
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final RankingService rankingService;
    private final StatisticsService statisticsService;

    /**
     * 개인 통계 조회
     * GET
     * /api/statistics/personal/{guildId}/{userId}?start=2025-01-01&end=2025-12-31
     */
    @GetMapping("/personal/{guildId}/{userId}")
    public PersonalStatsDto getPersonalStats(
            @PathVariable String guildId,
            @PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        // 기본값: 최근 30일
        LocalDateTime startDateTime = (start != null) ? start.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = (end != null) ? end.atTime(23, 59, 59) : LocalDateTime.now();

        return statisticsService.getPersonalStats(guildId, userId, startDateTime, endDateTime);
    }

    /**
     * 서버별 랭킹 조회
     * GET /api/statistics/ranking/{guildId}
     */
    @GetMapping("/ranking/{guildId}")
    public List<RankingDto> getServerRanking(@PathVariable String guildId) {
        return rankingService.getWeeklyRanking(guildId);
    }

    /**
     * 히트맵 데이터 조회
     * GET /api/statistics/heatmap/{guildId}/{userId}
     */
    @GetMapping("/heatmap/{guildId}/{userId}")
    public HeatmapDto getHeatmap(
            @PathVariable String guildId,
            @PathVariable String userId) {

        return statisticsService.getHeatmap(guildId, userId);
    }

    /**
     * 연속 기록(Streak) 조회
     * GET /api/statistics/streak/{guildId}/{userId}
     */
    @GetMapping("/streak/{guildId}/{userId}")
    public StreakDto getStreak(
            @PathVariable String guildId,
            @PathVariable String userId) {

        return statisticsService.getStreak(guildId, userId);
    }

    /**
     * 이벤트 랭킹 조회
     * GET /api/statistics/event-ranking/{guildId}
     */
    @GetMapping("/event-ranking/{guildId}")
    public List<RankingDto> getEventRanking(@PathVariable String guildId) {
        return rankingService.getEventRanking(guildId);
    }

    /**
     * GitHub 스타일 Contribution 히트맵 조회
     * GET /api/statistics/contribution/{guildId}/{userId}
     */
    @GetMapping("/contribution/{guildId}/{userId}")
    public ContributionHeatmapDto getContributionHeatmap(
            @PathVariable String guildId,
            @PathVariable String userId) {

        return statisticsService.getContributionHeatmap(guildId, userId);
    }
}
