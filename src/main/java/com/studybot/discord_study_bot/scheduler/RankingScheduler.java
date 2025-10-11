package com.studybot.discord_study_bot.scheduler;

import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.service.RankingService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;

@Component
public class RankingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RankingScheduler.class);
    private final JDA jda;
    private final RankingService rankingService;
    private final String TARGET_CHANNEL_NAME = "주간-랭킹";

    // 생성자를 직접 만들고, JDA 파라미터 앞에 @Lazy를 붙여서 JDA를 사용할 때 만듦
    public RankingScheduler(@Lazy JDA jda, RankingService rankingService) {
        this.jda = jda;
        this.rankingService = rankingService;
    }

    // 매주 월요일 오전 10시 (한국기준)에 실행
    @Scheduled(cron = "0 0 10 * * MON", zone = "Asia/Tokyo")
    public void postWeeklyRanking() {
        logger.info("주간 랭킹 자동 포스트 작업 시작");

        // 지난주 랭킹 데이터 가져옴
        List<RankingDto> previousWeeklyRanking = rankingService.getPreviousWeeklyRanking();

        if (previousWeeklyRanking.isEmpty()) {
            logger.info("지난주 공부 기록이 없습니다");
            return;
        }

        // "주간-랭킹" 채널 찾기
        List<TextChannel> channels = jda.getTextChannelsByName(TARGET_CHANNEL_NAME, true);
        if (channels.isEmpty()) {
            logger.warn("'{}' 채널을 찾을 수 없어 랭킹을 포스트할 수 없습니다.", TARGET_CHANNEL_NAME);
            return;
        }

        // 랭킹 메시지 만들기 (Embed 버전)
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🏆 지난주 공부 시간 랭킹 🏆");
        eb.setColor(new Color(0xF9E076)); // 황금색!

        StringBuilder description = new StringBuilder();

        // 단일 서버용: 첫 번째 채널의 길드 사용
        Guild guild = channels.get(0).getGuild();

        for (int i = 0; i < previousWeeklyRanking.size(); i++) {
            RankingDto ranker = previousWeeklyRanking.get(i);
            String userName;

            try {
                // 해당 서버에서 멤버 정보 가져오기
                Member member = guild.retrieveMemberById(ranker.getUserId()).complete();
                // 멤버의 서버 별명을 가져옴
                userName = member.getEffectiveName();

                description.append(String.format("%d. %s - %s\n",
                        i + 1,
                        userName,
                        formatDuration(ranker.getTotalDuration())));
            } catch (Exception e) {
                // 유저가 서버에 없는 경우
                userName = "(서버에 없는 사용자)";
                logger.warn("{} ID를 가진 유저가 서버에 없어서 이름을 찾을 수 없습니다.", ranker.getUserId());

                description.append(String.format("%d. %s - %s\n",
                        i + 1,
                        userName,
                        formatDuration(ranker.getTotalDuration())));
            }
        }
        eb.setDescription(description.toString());
        eb.setFooter("이번 주도 함께 달려봐요! 🔥");

        // 각 서버의 "주간-랭킹" 채널에 메시지 전송
        for (TextChannel channel : channels) {
            channel.sendMessageEmbeds(eb.build()).queue();
            logger.info("{} 서버의 {} 채널에 랭킹을 포스트했습니다.", channel.getGuild().getName(), channel.getName());
        }
    }

    // 매주 월요일 오전 10시 30분 (한국기준)에 실행 - 이벤트 랭킹
    @Scheduled(cron = "0 30 10 * * MON", zone = "Asia/Tokyo")
    public void postEventRanking() {
        logger.info("이벤트 랭킹 자동 포스트 작업 시작");

        // 이벤트 기간 체크
        if (!rankingService.isEventPeriod()) {
            logger.info("이벤트 기간이 아니므로 이벤트 랭킹을 포스트하지 않습니다.");
            return;
        }

        // 이벤트 기간 누계 랭킹 데이터 가져옴
        List<RankingDto> eventRanking = rankingService.getEventRanking();

        if (eventRanking.isEmpty()) {
            logger.info("이벤트 기간 공부 기록이 없습니다");
            return;
        }

        // "주간-랭킹" 채널 찾기
        List<TextChannel> channels = jda.getTextChannelsByName(TARGET_CHANNEL_NAME, true);
        if (channels.isEmpty()) {
            logger.warn("'{}' 채널을 찾을 수 없어 이벤트 랭킹을 포스트할 수 없습니다.", TARGET_CHANNEL_NAME);
            return;
        }

        // 랭킹 메시지 만들기 (Embed 버전)
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("🎉 이벤트 누계 공부 시간 랭킹 🎉");
        eb.setColor(new Color(0xFF6B6B)); // 빨간색!

        StringBuilder description = new StringBuilder();
        description.append("이벤트 기간: 2025년 10월 1일 ~ 12월 31일\n\n");

        // 단일 서버용: 첫 번째 채널의 길드 사용
        Guild guild = channels.get(0).getGuild();

        for (int i = 0; i < eventRanking.size(); i++) {
            RankingDto ranker = eventRanking.get(i);
            String userName;

            try {
                // 해당 서버에서 멤버 정보 가져오기
                Member member = guild.retrieveMemberById(ranker.getUserId()).complete();
                // 멤버의 서버 별명을 가져옴
                userName = member.getEffectiveName();

                description.append(String.format("%d. %s - %s\n",
                        i + 1,
                        userName,
                        formatDuration(ranker.getTotalDuration())));
            } catch (Exception e) {
                // 유저가 서버에 없는 경우
                userName = "(서버에 없는 사용자)";
                logger.warn("{} ID를 가진 유저가 서버에 없어서 이름을 찾을 수 없습니다.", ranker.getUserId());

                description.append(String.format("%d. %s - %s\n",
                        i + 1,
                        userName,
                        formatDuration(ranker.getTotalDuration())));
            }
        }
        eb.setDescription(description.toString());
        eb.setFooter("이벤트 상품을 향해 달려봐요! 🍗🏃‍♂️💨");

        // 각 서버의 "주간-랭킹" 채널에 메시지 전송
        for (TextChannel channel : channels) {
            channel.sendMessageEmbeds(eb.build()).queue();
            logger.info("{} 서버의 {} 채널에 이벤트 랭킹을 포스트했습니다.", channel.getGuild().getName(), channel.getName());
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
