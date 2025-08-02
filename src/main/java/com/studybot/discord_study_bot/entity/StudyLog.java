package com.studybot.discord_study_bot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity // JPA에 DB table의 설계도라고 알림
@Data // getter, setter 자동으로 만들어줌
public class StudyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String userId; // 디코 유저의 고유 ID
    private String userName; // 디코 유저의 이름
    private LocalDateTime startTime; // 공부 시작 시각
    private LocalDateTime endTime; // 공부 종료 시각
}
