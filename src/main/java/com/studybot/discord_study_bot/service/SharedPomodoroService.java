package com.studybot.discord_study_bot.service;

import com.studybot.discord_study_bot.i18n.MessageProvider;
import com.studybot.discord_study_bot.pomodoro.PomodoroState;
import com.studybot.discord_study_bot.pomodoro.SharedPomodoroSession;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 공유 뽀모도로 타이머 서비스
 */
@Service
public class SharedPomodoroService {

    private static final Logger logger = LoggerFactory.getLogger(SharedPomodoroService.class);
    private final JDA jda;
    private final StudySessionManager sessionManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    // Key: channelId (음성 채널 ID), Value: SharedPomodoroSession
    private final Map<String, SharedPomodoroSession> activeTimers = new ConcurrentHashMap<>();

    /**
     * 생성자 - JDA는 지연 로딩하여 순환 의존성 방지
     */
    public SharedPomodoroService(@Lazy JDA jda, StudySessionManager sessionManager) {
        this.jda = jda;
        this.sessionManager = sessionManager;
    }

    /**
     * 공유 타이머 시작
     */
    public void startSharedTimer(String voiceChannelId, String guildId, String textChannelId,
                                 int studyMinutes, int breakMinutes, boolean autoStart, String lang) {
        // 이미 타이머가 있으면 중지
        if (activeTimers.containsKey(voiceChannelId)) {
            stopSharedTimer(voiceChannelId);
        }

        SharedPomodoroSession session = new SharedPomodoroSession(voiceChannelId, guildId, textChannelId);
        session.setStudyMinutes(studyMinutes);
        session.setShortBreakMinutes(breakMinutes);
        session.setLongBreakMinutes(breakMinutes * 3); // 긴 휴식은 3배
        session.setAutoStart(autoStart);
        session.start();

        activeTimers.put(voiceChannelId, session);

        logger.info("[채널 ID: {}] 공유 뽀모도로 시작: {}분 공부 / {}분 휴식", 
            voiceChannelId, studyMinutes, breakMinutes);

        // 텍스트 채널에 타이머 메시지 표시
        sendSharedTimerMessage(session, lang);

        // 타이머 시작
        startTimerTick(session);
    }

    /**
     * 공유 타이머 중지
     */
    public void stopSharedTimer(String voiceChannelId) {
        SharedPomodoroSession session = activeTimers.get(voiceChannelId);
        if (session != null) {
            // 타이머 중지
            if (session.getTimerTask() != null) {
                session.getTimerTask().cancel(false);
            }

            // 모든 참여자의 뽀모도로 세션 종료
            for (String userId : session.getParticipants()) {
                sessionManager.pausePomodoro(session.getGuildId(), userId, userId);
            }

            activeTimers.remove(voiceChannelId);
            logger.info("[채널 ID: {}] 공유 뽀모도로 중지", voiceChannelId);
        }
    }

    /**
     * 참여자 추가
     */
    public void addParticipant(String voiceChannelId, String userId, String userName, String guildName, String lang) {
        SharedPomodoroSession session = activeTimers.get(voiceChannelId);
        if (session != null) {
            session.addParticipant(userId);

            // 공부 시간일 때만 StudyLog 시작
            if (session.getState() == PomodoroState.STUDY) {
                sessionManager.startPomodoroStudy(session.getGuildId(), guildName, userId, userName);
            }

            logger.info("{}님이 공유 뽀모도로에 참여했습니다.", userName);
            
            // 메시지 업데이트
            updateSharedTimerMessage(session, lang);
        }
    }

    /**
     * 참여자 제거
     */
    public void removeParticipant(String voiceChannelId, String userId, String userName, String lang) {
        SharedPomodoroSession session = activeTimers.get(voiceChannelId);
        if (session != null) {
            session.removeParticipant(userId);

            // StudyLog 종료
            sessionManager.pausePomodoro(session.getGuildId(), userId, userName);

            logger.info("{}님이 공유 뽀모도로에서 나갔습니다.", userName);
            
            // 메시지 업데이트
            updateSharedTimerMessage(session, lang);
        }
    }

    /**
     * 화면공유 상태 업데이트
     */
    public void updateScreenShareStatus(String voiceChannelId, String userId, boolean isSharing) {
        SharedPomodoroSession session = activeTimers.get(voiceChannelId);
        if (session != null) {
            session.updateScreenShareStatus(userId, isSharing);
        }
    }

    /**
     * 활성 세션 조회
     */
    public SharedPomodoroSession getActiveSession(String voiceChannelId) {
        return activeTimers.get(voiceChannelId);
    }

    /**
     * 타이머 일시정지
     */
    public void pauseTimer(String voiceChannelId, String lang) {
        SharedPomodoroSession session = activeTimers.get(voiceChannelId);
        if (session != null && session.getState() != PomodoroState.PAUSED) {
            session.setState(PomodoroState.PAUSED);
            
            // 타이머 중지 (스케줄러는 유지)
            logger.info("[채널 ID: {}] 공유 뽀모도로 일시정지", voiceChannelId);
            
            // 메시지 업데이트
            updateSharedTimerMessage(session, lang);
        }
    }

    /**
     * 타이머 재개
     */
    public void resumeTimer(String voiceChannelId, PomodoroState previousState, String lang) {
        SharedPomodoroSession session = activeTimers.get(voiceChannelId);
        if (session != null && session.getState() == PomodoroState.PAUSED) {
            session.setState(previousState);
            session.setPhaseStartTime(java.time.LocalDateTime.now());
            
            logger.info("[채널 ID: {}] 공유 뽀모도로 재개", voiceChannelId);
            
            // 메시지 업데이트
            updateSharedTimerMessage(session, lang);
        }
    }

    /**
     * 다음 단계로 건너뛰기
     */
    public void skipToNextPhase(String voiceChannelId, String lang) {
        SharedPomodoroSession session = activeTimers.get(voiceChannelId);
        if (session != null) {
            handlePhaseComplete(session, lang);
        }
    }

    /**
     * 타이머 틱 시작 (1초마다)
     */
    private void startTimerTick(SharedPomodoroSession session) {
        session.setTimerTask(scheduler.scheduleAtFixedRate(() -> {
            try {
                // 일시정지 상태면 스킵
                if (session.getState() == PomodoroState.PAUSED) {
                    return;
                }

                session.decrementSecond();

                // 5초마다 메시지 업데이트
                if (session.getRemainingSeconds() % 5 == 0) {
                    updateSharedTimerMessage(session, "ko"); // TODO: lang 저장 필요
                }

                // 시간 종료
                if (session.getRemainingSeconds() <= 0) {
                    handlePhaseComplete(session, "ko"); // TODO: lang 저장 필요
                }
            } catch (Exception e) {
                logger.error("타이머 틱 처리 중 오류 발생", e);
            }
        }, 1, 1, TimeUnit.SECONDS));
    }

    /**
     * 단계 완료 처리
     */
    private void handlePhaseComplete(SharedPomodoroSession session, String lang) {
        if (session.getState() == PomodoroState.STUDY) {
            // 공부 완료 → 휴식
            for (String userId : session.getParticipants()) {
                session.completeSet(userId);
                sessionManager.pausePomodoro(session.getGuildId(), userId, userId);
            }

            session.nextPhase();
            sendPhaseNotification(session, lang, "pomodoro.study_complete");

            // 공부 → 휴식은 항상 자동 시작
            logger.info("휴식 시간 자동 시작");
        } else {
            // 휴식 완료 → 공부
            session.nextPhase();
            sendPhaseNotification(session, lang, "pomodoro.break_complete");

            if (session.isAutoStart()) {
                // 자동 시작
                Guild guild = jda.getGuildById(session.getGuildId());
                if (guild != null) {
                    for (String userId : session.getParticipants()) {
                        Member member = guild.retrieveMemberById(userId).complete();
                        sessionManager.startPomodoroStudy(session.getGuildId(), 
                            session.getGuildId(), userId, member.getEffectiveName());
                    }
                }
                logger.info("자동 시작: 공부 시간 시작");
            } else {
                // 수동 시작 대기
                session.setState(PomodoroState.PAUSED);
            }
        }

        updateSharedTimerMessage(session, lang);
    }

    /**
     * 단계 완료 알림 전송
     */
    private void sendPhaseNotification(SharedPomodoroSession session, String lang, String messageKey) {
        TextChannel textChannel = jda.getTextChannelById(session.getTextChannelId());
        if (textChannel != null) {
            textChannel.sendMessage(MessageProvider.get(lang, messageKey)).queue();
        }
    }

    /**
     * 공유 타이머 메시지 전송
     */
    private void sendSharedTimerMessage(SharedPomodoroSession session, String lang) {
        TextChannel textChannel = jda.getTextChannelById(session.getTextChannelId());
        if (textChannel == null) {
            logger.warn("텍스트 채널을 찾을 수 없습니다: {}", session.getTextChannelId());
            return;
        }

        EmbedBuilder eb = buildTimerEmbed(session, lang);
        
        Button joinButton = Button.success("shared_join_" + session.getChannelId(), 
            MessageProvider.get(lang, "shared.btn.join"));
        Button leaveButton = Button.danger("shared_leave_" + session.getChannelId(), 
            MessageProvider.get(lang, "shared.btn.leave"));
        Button stopButton = Button.secondary("shared_stop_" + session.getChannelId(), 
            MessageProvider.get(lang, "shared.btn.stop"));

        textChannel.sendMessageEmbeds(eb.build())
            .addActionRow(joinButton, leaveButton, stopButton)
            .queue(message -> {
                session.setMessageId(message.getId());
                logger.info("공유 타이머 메시지 전송 완료: {}", message.getId());
            });
    }

    /**
     * 공유 타이머 메시지 업데이트
     */
    private void updateSharedTimerMessage(SharedPomodoroSession session, String lang) {
        if (session.getMessageId() == null) {
            return;
        }

        TextChannel textChannel = jda.getTextChannelById(session.getTextChannelId());
        if (textChannel == null) {
            return;
        }

        textChannel.retrieveMessageById(session.getMessageId()).queue(message -> {
            EmbedBuilder eb = buildTimerEmbed(session, lang);
            
            Button joinButton = Button.success("shared_join_" + session.getChannelId(), 
                MessageProvider.get(lang, "shared.btn.join"));
            Button leaveButton = Button.danger("shared_leave_" + session.getChannelId(), 
                MessageProvider.get(lang, "shared.btn.leave"));
            Button stopButton = Button.secondary("shared_stop_" + session.getChannelId(), 
                MessageProvider.get(lang, "shared.btn.stop"));

            // 일시정지 상태면 버튼 변경
            if (session.getState() == PomodoroState.PAUSED) {
                Button resumeButton = Button.primary("shared_resume_" + session.getChannelId(), 
                    MessageProvider.get(lang, "pomodoro.btn.resume"));
                message.editMessageEmbeds(eb.build())
                    .setActionRow(resumeButton, stopButton)
                    .queue();
            } else {
                Button pauseButton = Button.secondary("shared_pause_" + session.getChannelId(), 
                    MessageProvider.get(lang, "pomodoro.btn.pause"));
                message.editMessageEmbeds(eb.build())
                    .setActionRow(joinButton, leaveButton, pauseButton, stopButton)
                    .queue();
            }
        }, error -> logger.warn("메시지 업데이트 실패: {}", session.getMessageId()));
    }

    /**
     * 타이머 Embed 생성
     */
    private EmbedBuilder buildTimerEmbed(SharedPomodoroSession session, String lang) {
        EmbedBuilder eb = new EmbedBuilder();

        // 음성 채널 이름 조회
        VoiceChannel voiceChannel = jda.getVoiceChannelById(session.getChannelId());
        String channelName = voiceChannel != null ? voiceChannel.getName() : "Unknown";

        eb.setTitle(MessageProvider.format(lang, "shared.pomodoro_title", channelName));

        // 상태별 색상
        Color color = switch (session.getState()) {
            case STUDY -> new Color(0xED4245); // Discord Red
            case SHORT_BREAK, LONG_BREAK -> new Color(0x57F287); // Discord Green
            case PAUSED -> new Color(0xFEE75C); // Discord Yellow
        };
        eb.setColor(color);

        // 상태 표시
        String phaseText = switch (session.getState()) {
            case STUDY -> MessageProvider.get(lang, "pomodoro.study_phase");
            case SHORT_BREAK -> MessageProvider.get(lang, "pomodoro.break_phase");
            case LONG_BREAK -> MessageProvider.get(lang, "pomodoro.long_break_phase");
            case PAUSED -> MessageProvider.get(lang, "pomodoro.paused");
        };

        StringBuilder description = new StringBuilder();
        description.append(phaseText).append("\n");

        // 시간 표시
        if (session.getState() == PomodoroState.PAUSED) {
            description.append(MessageProvider.format(lang, "pomodoro.remaining_time_paused", 
                session.getFormattedRemainingTime()));
        } else {
            description.append(MessageProvider.format(lang, "pomodoro.remaining_time", 
                session.getFormattedRemainingTime(), 
                session.getFormattedTotalTime()));
        }

        description.append("\n");

        // 세트 진행도
        if (session.getState() == PomodoroState.STUDY) {
            description.append(MessageProvider.format(lang, "pomodoro.set_progress", 
                session.getCurrentSet(), session.getTotalSets()));
        } else if (session.getState() != PomodoroState.PAUSED) {
            description.append(MessageProvider.format(lang, "pomodoro.set_complete", 
                session.getCurrentSet() - 1, session.getTotalSets()));
        }

        eb.setDescription(description.toString());

        // 참여자 목록
        if (!session.getParticipants().isEmpty()) {
            StringBuilder participants = new StringBuilder();
            participants.append(MessageProvider.format(lang, "shared.participants", 
                session.getParticipants().size())).append("\n");

            Guild guild = jda.getGuildById(session.getGuildId());
            if (guild != null) {
                for (String userId : session.getParticipants()) {
                    try {
                        Member member = guild.retrieveMemberById(userId).complete();
                        String screenShareIcon = session.getScreenShareStatus().getOrDefault(userId, false) ? " 🎥" : "";
                        int completedSets = session.getCompletedSets().getOrDefault(userId, 0);
                        
                        participants.append(String.format("• %s%s (%d세트 완료)\n", 
                            member.getEffectiveName(), screenShareIcon, completedSets));
                    } catch (Exception e) {
                        logger.warn("멤버 정보 조회 실패: {}", userId);
                    }
                }
            }

            eb.addField("", participants.toString(), false);
        }

        eb.setFooter(MessageProvider.get(lang, "pomodoro.footer"));
        eb.setTimestamp(java.time.Instant.now());

        return eb;
    }

}
