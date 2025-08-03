package com.studybot.discord_study_bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 만들어주는 Lombok 어노테이션
public class RankingDto {
    private String userName;
    private Long totalDuration;
}
