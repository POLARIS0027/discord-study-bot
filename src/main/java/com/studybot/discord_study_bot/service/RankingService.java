package com.studybot.discord_study_bot.service;

import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<RankingDto> getWeeklyRanking() {
        LocalDateTime startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59);

        // DB로부터 순수 데이터(Object 배열의 리스트)를 받습니다.
        List<Object[]> rawRankingData = studyLogRepository.findRankingsByPeriod(startOfWeek, endOfWeek);

        // 순수 데이터를 RankingDto 리스트로 변환합니다.
        return rawRankingData.stream()
                .map(data -> new RankingDto(
                        (String) data[0], // 첫 번째 값(user_name)은 String으로
                        ((BigDecimal) data[1]).longValue() // 두 번째 값(SUM 결과)은 BigDecimal -> Long으로 변환
                ))
                .collect(Collectors.toList());
    }
}
