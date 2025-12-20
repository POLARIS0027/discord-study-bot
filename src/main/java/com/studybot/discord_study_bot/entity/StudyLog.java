package com.studybot.discord_study_bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Data;

import java.time.LocalDateTime;

@Entity // JPA에 DB table의 설계도라고 알림
@Data // getter, setter 자동으로 만들어줌
public class StudyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String guildId; // Discord 서버(길드) ID
    private String guildName; // Discord 서버 이름
    private String userId; // 디코 유저의 고유 ID
    private String userName; // 디코 유저의 이름
    private LocalDateTime startTime; // 공부 시작 시각
    private LocalDateTime endTime; // 공부 종료 시각

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 레코드 생성 시각

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
