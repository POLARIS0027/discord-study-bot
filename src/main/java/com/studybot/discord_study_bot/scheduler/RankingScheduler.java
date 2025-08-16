package com.studybot.discord_study_bot.scheduler;

import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;


@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RankingScheduler.class);
    private final JDA jda;
    private final RankingService rankingService;
    private final String TARGET_CHANNEL_NAME = "주간-랭킹";

    // 매주 월요일 오전 10시 (한국기준)에 실행
    @Scheduled(cron = "0 0 10 * * MON", zone ="Asia/Tokyo")
    public void postWeeklyRanking() {
        logger.info("주간 랭킹 자동 포스트 작업 시작");

        // 지난주 랭킹 데이터 가져옴
        List<RankingDto> previousWeeklyRanking = rankingService.getPreviousWeeklyRanking();

        if (previousWeeklyRanking.isEmpty()) {
            logger.info("지난주 공부 기록이 없습니다");
            return;
        }

        // 랭킹 메시지 만들기 (Embed 버전)
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🏆 지난주 공부 시간 랭킹 🏆");
        eb.setColor(new Color(0xF9E076)); // 황금색!

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < previousWeeklyRanking.size(); i++) {
            RankingDto ranker = previousWeeklyRanking.get(i);

            // ID로 최신 유저 정보 가져오기
            try {
                User user = jda.retrieveUserById(ranker.getUserId()).complete();
                String userName = user != null ? user.getEffectiveName() : "(알 수 없는 사용자)";

                description.append(String.format("%d. %s - %s\n",
                        i + 1,
                        userName,
                        formatDuration(ranker.getTotalDuration())));
            } catch (Exception e) {
                logger.warn("{} ID를 가진 유저를 찾을 수 없어 랭킹에서 제외합니다.", ranker.getUserId());
            }
        }
        eb.setDescription(description.toString());
        eb.setFooter("이번 주도 함께 달려봐요! 🔥");

        // 3. "주간-랭킹" 채널 찾아서 메시지 보내기
        List<TextChannel> channels = jda.getTextChannelsByName(TARGET_CHANNEL_NAME, true);
        if (channels.isEmpty()) {
            logger.warn("'{}' 채널을 찾을 수 없어 랭킹을 포스트할 수 없습니다.", TARGET_CHANNEL_NAME);
            return;
        }

        for (TextChannel channel : channels) {
            channel.sendMessageEmbeds(eb.build()).queue();
            logger.info("{} 서버의 {} 채널에 랭킹을 포스트했습니다.", channel.getGuild().getName(), channel.getName());
        }
    }

    // 시간 포맷을 위한 헬퍼 메서드 (RankingCommandListener이랑 동일)
    private String formatDuration(long totalSeconds) {
        if (totalSeconds < 60) {
            return String.format("%d초", totalSeconds);
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d시간 %d분 %d초", hours, minutes, seconds);
        } else {
            return String.format("%d분 %d초", minutes, seconds);
        }
    }
}
