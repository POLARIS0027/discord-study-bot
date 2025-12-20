package com.studybot.discord_study_bot.service;

import com.studybot.discord_study_bot.dto.ContributionHeatmapDto;
import com.studybot.discord_study_bot.dto.HeatmapDto;
import com.studybot.discord_study_bot.dto.PersonalStatsDto;
import com.studybot.discord_study_bot.dto.StreakDto;
import com.studybot.discord_study_bot.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StudyLogRepository studyLogRepository;

    /**
     * 개인 통계 조회
     */
    public PersonalStatsDto getPersonalStats(String guildId, String userId, LocalDateTime start, LocalDateTime end) {
        // 일별 공부 시간 조회
        List<Object[]> dailyData = studyLogRepository.findDailyStudyTime(guildId, userId, start, end);

        List<PersonalStatsDto.DailyStudyDto> dailyStats = dailyData.stream()
                .map(data -> {
                    String date = data[0].toString();
                    Long studyTime = data[1] != null ? ((BigDecimal) data[1]).longValue() : 0L;
                    return new PersonalStatsDto.DailyStudyDto(date, studyTime);
                })
                .collect(Collectors.toList());

        // 총 공부 시간 계산
        Long totalStudyTime = dailyStats.stream()
                .mapToLong(PersonalStatsDto.DailyStudyDto::getStudyTime)
                .sum();

        // 사용자 이름 및 길드 이름 조회 (최근 레코드에서)
        String userName = studyLogRepository.findTopByGuildIdAndUserIdOrderByIdDesc(guildId, userId)
                .map(log -> log.getUserName())
                .orElse(null);

        String guildName = studyLogRepository.findTopByGuildIdAndUserIdOrderByIdDesc(guildId, userId)
                .map(log -> log.getGuildName())
                .orElse(null);

        return new PersonalStatsDto(userId, userName, guildId, guildName, totalStudyTime, dailyStats);
    }

    /**
     * 히트맵 데이터 조회
     */
    public HeatmapDto getHeatmap(String guildId, String userId) {
        List<Object[]> heatmapData = studyLogRepository.findStudyPatternHeatmap(guildId, userId);

        List<HeatmapDto.HeatmapCell> cells = heatmapData.stream()
                .map(data -> {
                    int hour = ((Number) data[0]).intValue();
                    int dayOfWeek = ((Number) data[1]).intValue();
                    long count = ((Number) data[2]).longValue();
                    return new HeatmapDto.HeatmapCell(hour, dayOfWeek, count);
                })
                .collect(Collectors.toList());

        // 사용자 이름 조회
        String userName = studyLogRepository.findTopByGuildIdAndUserIdOrderByIdDesc(guildId, userId)
                .map(log -> log.getUserName())
                .orElse(null);

        return new HeatmapDto(userId, userName, cells);
    }

    /**
     * 연속 기록(Streak) 조회
     */
    public StreakDto getStreak(String guildId, String userId) {
        // 최근 1년 데이터 조회
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<String> studyDates = studyLogRepository.findStudyDates(guildId, userId, oneYearAgo);

        // 사용자 이름 조회
        String userName = studyLogRepository.findTopByGuildIdAndUserIdOrderByIdDesc(guildId, userId)
                .map(log -> log.getUserName())
                .orElse(null);

        if (studyDates.isEmpty()) {
            return new StreakDto(userId, userName, 0, 0, studyDates);
        }

        // 현재 연속 기록 계산
        int currentStreak = calculateCurrentStreak(studyDates);

        // 최장 연속 기록 계산
        int longestStreak = calculateLongestStreak(studyDates);

        return new StreakDto(userId, userName, currentStreak, longestStreak, studyDates);
    }

    /**
     * 현재 연속 기록 계산 (오늘 또는 어제부터 역순으로)
     */
    private int calculateCurrentStreak(List<String> studyDates) {
        if (studyDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastDate = LocalDate.parse(studyDates.get(studyDates.size() - 1));

        // 마지막 공부 날짜가 오늘이나 어제가 아니면 연속 기록 끊김
        if (lastDate.isBefore(today.minusDays(1))) {
            return 0;
        }

        int streak = 0;
        LocalDate checkDate = lastDate;

        for (int i = studyDates.size() - 1; i >= 0; i--) {
            LocalDate studyDate = LocalDate.parse(studyDates.get(i));

            if (studyDate.equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (studyDate.isBefore(checkDate)) {
                // 날짜가 연속되지 않으면 중단
                break;
            }
        }

        return streak;
    }

    /**
     * 최장 연속 기록 계산
     */
    private int calculateLongestStreak(List<String> studyDates) {
        if (studyDates.isEmpty()) {
            return 0;
        }

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < studyDates.size(); i++) {
            LocalDate prevDate = LocalDate.parse(studyDates.get(i - 1));
            LocalDate currDate = LocalDate.parse(studyDates.get(i));

            if (currDate.equals(prevDate.plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    /**
     * GitHub 스타일 Contribution 히트맵 조회
     */
    public ContributionHeatmapDto getContributionHeatmap(String guildId, String userId) {
        // 최근 1년 데이터 조회
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        LocalDateTime now = LocalDateTime.now();

        List<Object[]> dailyData = studyLogRepository.findDailyStudyTime(guildId, userId, oneYearAgo, now);

        // 날짜별 공부 시간을 Map으로 변환
        Map<String, Long> studyTimeMap = new HashMap<>();
        for (Object[] data : dailyData) {
            String date = data[0].toString();
            Long studyTime = data[1] != null ? ((BigDecimal) data[1]).longValue() : 0L;
            studyTimeMap.put(date, studyTime);
        }

        // 최근 1년 전체 날짜를 생성하고 contribution 데이터 생성
        List<ContributionHeatmapDto.DailyContribution> contributions = new ArrayList<>();
        LocalDate startDate = oneYearAgo.toLocalDate();
        LocalDate endDate = now.toLocalDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.toString();
            Long studyTime = studyTimeMap.getOrDefault(dateStr, 0L);
            int level = calculateLevel(studyTime);

            contributions.add(new ContributionHeatmapDto.DailyContribution(dateStr, studyTime, level));
        }

        // 사용자 이름 조회
        String userName = studyLogRepository.findTopByGuildIdAndUserIdOrderByIdDesc(guildId, userId)
                .map(log -> log.getUserName())
                .orElse(null);

        return new ContributionHeatmapDto(userId, userName, contributions);
    }

    /**
     * 공부 시간(초)에 따라 레벨 계산 (GitHub 스타일)
     * 
     * @param studyTimeInSeconds 공부 시간 (초)
     * @return 0 (없음) ~ 4 (매우 많음)
     */
    private int calculateLevel(Long studyTimeInSeconds) {
        if (studyTimeInSeconds == 0) {
            return 0;
        }

        // 시간 단위로 변환
        double hours = studyTimeInSeconds / 3600.0;

        if (hours < 1.0) {
            return 1; // 1시간 미만
        } else if (hours < 2.0) {
            return 2; // 1~2시간
        } else if (hours < 4.0) {
            return 3; // 2~4시간
        } else {
            return 4; // 4시간 이상
        }
    }
}
