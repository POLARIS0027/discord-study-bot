package com.studybot.discord_study_bot.config;

import com.studybot.discord_study_bot.listener.VoiceChannelListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 설정을 담당하는 클래스라고 알려줌
@RequiredArgsConstructor
public class JdaConfig {

    //application.yaml에서 bot token가져옴
    @Value("${discord.token}")
    private String token;

    private final VoiceChannelListener voiceChannelListener;

    @Bean // JDA 객체를 spring이 관리하도록 함
    public JDA jda() throws InterruptedException{
        JDA jda = JDABuilder.createDefault(token)
                // 음성감지 권한
                .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                // Listener를 봇에 등록
                .addEventListeners(voiceChannelListener)
                .build();

        // 봇 빌드 대기
        jda.awaitReady();

        return jda;
    }
}
