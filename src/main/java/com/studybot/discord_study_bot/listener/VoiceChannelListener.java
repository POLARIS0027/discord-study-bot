package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.entity.StudyLog;
import com.studybot.discord_study_bot.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceStreamEvent;
import org.slf4j.Logger; // slf4j 임포트
import org.slf4j.LoggerFactory; // slf4j 임포트

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Component // 이 클래스도 스프링이 관리하는 부품이에요!
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줘요.
public class VoiceChannelListener extends ListenerAdapter {

    // Logger 객체 생성
    private static final Logger logger = LoggerFactory.getLogger(VoiceChannelListener.class);

    // final로 선언해서 바뀌지 않게 함
    private final StudyLogRepository studyLogRepository;

    // 상태를 관리할 Map
    // Key : userId, Value: StudyLog객체
    private final Map<String, StudyLog> activeStudySessions = new ConcurrentHashMap<>();

    // Stream을 감시함
    @Override
    @Transactional
    public void onGuildVoiceStream(GuildVoiceStreamEvent event) {
        String userId = event.getMember().getId();
        // 봇인 경우는 무시함
        if (event.getMember().getUser().isBot()) {
            return;
        }

        // 화면공유 상태 취득
        boolean isStreaming = event.getVoiceState().isStream();

        // 화면 공유가 시작되면
        if (isStreaming) {
            logger.info("{}님이 화면 공유를 시작했습니다.", event.getMember().getEffectiveName());
            startStudyLog(userId, event.getMember().getEffectiveName());
        } else { // 화면 공유가 끝나면
            logger.info("{}님이 화면 공유를 종료했습니다.", event.getMember().getEffectiveName());
            closeStudyLog(userId, event.getMember().getEffectiveName());
        }
    }

    @Override
    @Transactional
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        String userId = event.getMember().getId();
        // 봇이면 무시
        if (event.getMember().getUser().isBot()) {
            return;
        }

        // 채널에서 나갔을 경우
        boolean joinedToChannel = event.getChannelLeft() == null && event.getChannelJoined() != null;
        boolean leftFromChannel = event.getChannelJoined() == null && event.getChannelLeft() != null;

        if (joinedToChannel){
            logger.info("{}님이 음성 채널에 들어왔습니다.", event.getMember().getEffectiveName());
        }
        if (leftFromChannel) {
            logger.info("{}님이 음성 채널에서 나갔습니다.", event.getMember().getEffectiveName());
        }
        // Map에 기록에 있는 사람일때만 closeStudyLog 실행
        if (leftFromChannel && activeStudySessions.containsKey(userId)) {
            closeStudyLog(userId, event.getMember().getEffectiveName());
        }
    }

    // 공부 시작 로직
    private void startStudyLog(String userId, String userName) {
        logger.info("{}({})님의 공부시간 기록을 시작합니다.", userName, userId);
        // 만약 공부중인 기록이 있다면 일단 종료부터 함.
        if (activeStudySessions.containsKey(userId)) {
            closeStudyLog(userId, userName);
        }

        StudyLog newLog = new StudyLog();
        newLog.setUserId(userId);
        newLog.setUserName(userName);
        newLog.setStartTime(LocalDateTime.now());

        StudyLog savedLog = studyLogRepository.save(newLog);

        // DB에 저장 후, MAP에 기록
        activeStudySessions.put(userId, savedLog);

    }
    // 공부 종료 로직
    private void closeStudyLog(String userId, String userName) {
        // Map에서 바로 가져옴
        StudyLog log = activeStudySessions.get(userId);

        if (log != null && log.getEndTime() == null) {

            LocalDateTime startTime = log.getStartTime();
            LocalDateTime endTime = LocalDateTime.now();

            // 시작 날짜와 종료 날짜의 주가 다른지 확인함
            LocalDate startDay = startTime.toLocalDate();
            LocalDate endDay = endTime.toLocalDate();
            LocalDate startOfWeekForStartDay = startDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate startOfWeekForEndDay = endDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            if (!startOfWeekForStartDay.isEqual(startOfWeekForEndDay)) {
                // 다음 주로 넘어간 경우 -> 기록을 2개로 분할함
                logger.info("주가 변경되어 공부 기록을 분할합니다: {} -> {}", startDay, endDay);

                // 지난주 기록 : 원래 시작시간 ~ 월요일 1초전까지
                LocalDateTime endOfLastWeek = startOfWeekForEndDay.atStartOfDay().minusNanos(1);
                log.setEndTime(endOfLastWeek);
                studyLogRepository.save(log);

                // 이번주 기록 : 주가 바뀌는 시점 (월요일 0시 ~ 종료시까지 생성)
                StudyLog newWeekLog = new StudyLog();
                newWeekLog.setUserId(log.getUserId());
                newWeekLog.setUserName(log.getUserName());
                newWeekLog.setStartTime(startOfWeekForEndDay.atStartOfDay());
                newWeekLog.setEndTime(endTime);
                studyLogRepository.save(newWeekLog);
            } else {
                // 같은 주일 경우
                log.setEndTime(endTime);
                studyLogRepository.save(log);
            }
            logger.info("ID {}({})님의 공부 기록을 종료했습니다. Map에서 삭제합니다.", userName, userId);

            // Map에서 삭제
            activeStudySessions.remove((userId));
        }
    }
}