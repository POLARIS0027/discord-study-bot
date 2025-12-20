package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.entity.StudyLog;
import com.studybot.discord_study_bot.repository.StudyLogRepository;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class VoiceChannelListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VoiceChannelListener.class);
    private final StudyLogRepository studyLogRepository;

    // 상태를 관리할 Map - 멀티 서버 지원을 위해 "guildId:userId" 형태의 키 사용
    // Key : "guildId:userId", Value: StudyLog객체
    private final Map<String, StudyLog> activeStudySessions = new ConcurrentHashMap<>();

    // Stream을 감시함
    @Override
    @Transactional
    public void onGuildVoiceStream(GuildVoiceStreamEvent event) {
        String guildId = event.getGuild().getId();
        String guildName = event.getGuild().getName();
        String userId = event.getMember().getId();
        String userName = event.getMember().getEffectiveName();

        // 봇인 경우는 무시함
        if (event.getMember().getUser().isBot()) {
            return;
        }

        // 화면공유 상태 취득
        boolean isStreaming = event.getVoiceState().isStream();

        // 화면 공유가 시작되면
        if (isStreaming) {
            logger.info("[{}] {}님이 화면 공유를 시작했습니다.", guildName, userName);
            startStudyLog(guildId, guildName, userId, userName);
        } else { // 화면 공유가 끝나면
            logger.info("[{}] {}님이 화면 공유를 종료했습니다.", guildName, userName);
            closeStudyLog(guildId, userId, userName);
        }
    }

    @Override
    @Transactional
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        String guildId = event.getGuild().getId();
        String guildName = event.getGuild().getName();
        String userId = event.getMember().getId();
        String userName = event.getMember().getEffectiveName();

        // 봇이면 무시
        if (event.getMember().getUser().isBot()) {
            return;
        }

        // 채널에서 나갔을 경우
        boolean joinedToChannel = event.getChannelLeft() == null && event.getChannelJoined() != null;
        boolean leftFromChannel = event.getChannelJoined() == null && event.getChannelLeft() != null;

        if (joinedToChannel) {
            logger.info("[{}] {}님이 음성 채널에 들어왔습니다.", guildName, userName);
        }
        if (leftFromChannel) {
            logger.info("[{}] {}님이 음성 채널에서 나갔습니다.", guildName, userName);
        }

        // Map에 기록에 있는 사람일때만 closeStudyLog 실행
        String sessionKey = guildId + ":" + userId;
        if (leftFromChannel && activeStudySessions.containsKey(sessionKey)) {
            closeStudyLog(guildId, userId, userName);
        }
    }

    // 공부 시작 로직
    private void startStudyLog(String guildId, String guildName, String userId, String userName) {
        logger.info("[{}] {}({})님의 공부시간 기록을 시작합니다.", guildName, userName, userId);

        // 멀티 서버 지원을 위한 세션 키 생성
        String sessionKey = guildId + ":" + userId;

        // 만약 이 서버에서 공부중인 기록이 있다면 일단 종료부터 함.
        if (activeStudySessions.containsKey(sessionKey)) {
            closeStudyLog(guildId, userId, userName);
        }

        StudyLog newLog = new StudyLog();
        newLog.setGuildId(guildId);
        newLog.setGuildName(guildName);
        newLog.setUserId(userId);
        newLog.setUserName(userName);
        newLog.setStartTime(LocalDateTime.now());

        StudyLog savedLog = studyLogRepository.save(newLog);

        // DB에 저장 후, MAP에 기록
        activeStudySessions.put(sessionKey, savedLog);
    }

    // 공부 종료 로직
    private void closeStudyLog(String guildId, String userId, String userName) {
        // 멀티 서버 지원을 위한 세션 키 생성
        String sessionKey = guildId + ":" + userId;

        // Map에서 바로 가져옴
        StudyLog log = activeStudySessions.get(sessionKey);

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
                logger.info("[서버ID: {}] 주가 변경되어 공부 기록을 분할합니다: {} -> {}", guildId, startDay, endDay);

                // 지난주 기록 : 원래 시작시간 ~ 월요일 1초전까지
                LocalDateTime endOfLastWeek = startOfWeekForEndDay.atStartOfDay().minusNanos(1);
                log.setEndTime(endOfLastWeek);
                studyLogRepository.save(log);

                // 이번주 기록 : 주가 바뀌는 시점 (월요일 0시 ~ 종료시까지 생성)
                StudyLog newWeekLog = new StudyLog();
                newWeekLog.setGuildId(log.getGuildId());
                newWeekLog.setGuildName(log.getGuildName());
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
            logger.info("[서버ID: {}] {}({})님의 공부 기록을 종료했습니다. Map에서 삭제합니다.", guildId, userName, userId);

            // Map에서 삭제
            activeStudySessions.remove(sessionKey);
        }
    }
}