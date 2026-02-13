package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.service.SharedPomodoroService;
import com.studybot.discord_study_bot.service.StudySessionManager;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class VoiceChannelListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VoiceChannelListener.class);
    private final StudySessionManager sessionManager;
    private final SharedPomodoroService sharedPomodoroService;

    // Stream을 감시함
    @Override
    public void onGuildVoiceStream(@NotNull GuildVoiceStreamEvent event) {
        String guildId = event.getGuild().getId();
        String guildName = event.getGuild().getName();
        String userId = event.getMember().getId();
        String userName = event.getMember().getEffectiveName();

        // 봇인 경우는 무시함
        if (event.getMember().getUser().isBot()) {
            return;
        }

        // 화면공유 상태 취득
        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null) {
            return;
        }
        boolean isStreaming = voiceState.isStream();

        // 화면 공유가 시작되면
        if (isStreaming) {
            logger.info("[{}] {}님이 화면 공유를 시작했습니다.", guildName, userName);
            sessionManager.startScreenShare(guildId, guildName, userId, userName);
            
            // 공유 뽀모도로 참여 중이면 화면공유 상태 업데이트
            if (voiceState.getChannel() != null) {
                VoiceChannel voiceChannel = voiceState.getChannel().asVoiceChannel();
                sharedPomodoroService.updateScreenShareStatus(voiceChannel.getId(), userId, true);
            }
        } else { // 화면 공유가 끝나면
            logger.info("[{}] {}님이 화면 공유를 종료했습니다.", guildName, userName);
            sessionManager.stopScreenShare(guildId, userId, userName);
            
            // 공유 뽀모도로 화면공유 상태 업데이트
            if (voiceState.getChannel() != null) {
                VoiceChannel voiceChannel = voiceState.getChannel().asVoiceChannel();
                sharedPomodoroService.updateScreenShareStatus(voiceChannel.getId(), userId, false);
            }
        }
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        String guildId = event.getGuild().getId();
        String guildName = event.getGuild().getName();
        String userId = event.getMember().getId();
        String userName = event.getMember().getEffectiveName();
        User user = event.getMember().getUser();

        // 봇이면 무시
        if (user.isBot()) {
            return;
        }

        // 채널에서 나갔을 경우
        VoiceChannel joinedChannel = null;
        VoiceChannel leftChannel = null;
        
        if (event.getChannelJoined() != null) {
            joinedChannel = event.getChannelJoined().asVoiceChannel();
        }
        if (event.getChannelLeft() != null) {
            leftChannel = event.getChannelLeft().asVoiceChannel();
        }
        
        boolean joinedToChannel = leftChannel == null && joinedChannel != null;
        boolean leftFromChannel = joinedChannel == null && leftChannel != null;

        if (joinedToChannel && joinedChannel != null) {
            logger.info("[{}] {}님이 음성 채널에 들어왔습니다.", guildName, userName);
            
            // 공유 뽀모도로 진행 중인 채널인지 확인
            String lang = "ko"; // TODO: 사용자 언어 감지
            sharedPomodoroService.sendJoinInvitationOnChannelJoin(user, joinedChannel.getId(), 
                joinedChannel.getName(), lang);
        }
        
        if (leftFromChannel) {
            logger.info("[{}] {}님이 음성 채널에서 나갔습니다.", guildName, userName);
            
            // 모든 세션 강제 종료 (화면공유 + 뽀모도로)
            sessionManager.forceCloseSession(guildId, userId, userName);
        }
    }

}