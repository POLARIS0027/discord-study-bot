package com.studybot.discord_study_bot.service;

import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final StudyLogRepository studyLogRepository;

    @Value("${discord.exclude-user-id")
    private String excludeUserId;

    // 이번주의 요청받은 시점까지의 랭킹을 표시함. !주간랭킹
    public List<RankingDto> getWeeklyRanking() {
        LocalDateTime startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59);

        // DB로부터 순수 데이터(Object 배열의 리스트)를 받아옴
        List<Object[]> rawRankingData = studyLogRepository.findRankingsByPeriod(startOfWeek, endOfWeek, excludeUserId);

        // 순수 데이터를 RankingDto 리스트로 변환
        return rawRankingData.stream()
                .map(data -> new RankingDto(
                        (String) data[0], // 첫 번째 값(user_id)
                        ((BigDecimal) data[1]).longValue() // 두 번째 값(SUM 결과)은 BigDecimal -> Long으로 변환
                ))
                .collect(Collectors.toList());
    }

    // 지난주 랭킹을 계산함
    public  List<RankingDto> getPreviousWeeklyRanking() {
        // 지난주 일요일 날짜를 구함
        LocalDate lastSunday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        // 지난 주 일요일이 속한 주의 월요일 날짜를 구함
        LocalDate lastMonday = lastSunday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 집계구간 적용. 지난주 월 0시 0분~지난주 일 23시 59분까지
        LocalDateTime startOfLastWeek = lastMonday.atStartOfDay();
        LocalDateTime endOfLastWeek = lastSunday.atTime(23, 59,59);

        // getWeeklyRanking 사용
        List<Object[]> rawRankingData = studyLogRepository.findRankingsByPeriod(startOfLastWeek, endOfLastWeek, excludeUserId);
        return rawRankingData.stream()
                .map(data -> new RankingDto(
                        (String) data[0],
                        ((BigDecimal) data[1]).longValue()
                ))
                .collect(Collectors.toList());
    }
}
