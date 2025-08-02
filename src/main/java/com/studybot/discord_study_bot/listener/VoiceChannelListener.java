package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.entity.StudyLog;
import com.studybot.discord_study_bot.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger; // slf4j 임포트
import org.slf4j.LoggerFactory; // slf4j 임포트

import java.time.LocalDateTime;
import java.util.Optional;


@Component // 이 클래스도 스프링이 관리하는 부품이에요!
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줘요.
public class VoiceChannelListener extends ListenerAdapter {

    // Logger 객체 생성
    private static final Logger logger = LoggerFactory.getLogger(VoiceChannelListener.class);

    // final로 선언해서, 한번 정해지면 바뀌지 않는 창고 관리인을 데려와요.
    private final StudyLogRepository studyLogRepository;

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        // 봇이 채널에 들어온 경우는 무시함.
        if (event.getMember().getUser().isBot()) {
            return;
        }

        boolean joined = event.getChannelLeft() == null && event.getChannelJoined() != null;
        boolean left = event.getChannelJoined() == null && event.getChannelLeft() != null;

        // 음성채널 접속시
        if (joined) {
            logger.info("{}님이 {} 채널에서 공부를 시작했습니다.", event.getMember().getEffectiveName(), event.getChannelJoined().getName());

            StudyLog newLog = new StudyLog();
            newLog.setUserId(event.getMember().getId());
            newLog.setUserName(event.getMember().getEffectiveName());
            newLog.setStartTime(LocalDateTime.now());

            studyLogRepository.save(newLog);
        } else if (left) { // 나갔을 때
            logger.info("{}님이 {} 채널에서 공부를 마쳤습니다.", event.getMember().getEffectiveName(), event.getChannelLeft().getName());

            // 없을수도 있으니, optional을 사용해준다.
            Optional<StudyLog> optionalLog = studyLogRepository.findLatestUnfinishedLogByUserId(event.getMember().getId());

            // optionalLog의 내용물이 있다면. 그걸 log라고 정하고, setEndTime을 한다.
            optionalLog.ifPresent(log ->{
                log.setEndTime(LocalDateTime.now());
                studyLogRepository.save(log);
            });
        }
    }
}