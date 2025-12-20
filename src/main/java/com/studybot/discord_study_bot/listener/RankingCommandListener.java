package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.config.CommandConfig;
import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.i18n.MessageProvider;
import com.studybot.discord_study_bot.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Value;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RankingCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RankingCommandListener.class);
    private final RankingService rankingService;

    @Value("${discord.prefix}")
    private String prefix;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 봇이 보낸 메세지나 서버에서 온 메세지가 아니면 무시
        if (event.getAuthor().isBot() || !event.isFromGuild()) {
            return;
        }

        String message = event.getMessage().getContentRaw();
        User author = event.getAuthor();
        String authorId = author.getId();
        Guild guild = event.getGuild();
        String guildId = guild.getId();

        // prefix로 시작하지 않으면 무시함
        if (!message.startsWith(prefix)) {
            return;
        }

        // 명령어 추출
        String command = message.substring(prefix.length());

        // 명령어 언어 및 액션 타입 가져오기
        String lang = CommandConfig.getLanguage(command);
        String action = CommandConfig.getAction(command);

        if (action == null) {
            return; // 알 수 없는 명령어는 무시
        }

        logger.info("[{}] {} 명령어 요청을 받았습니다. (언어: {})", guild.getName(), command, lang);

        switch (action) {
            case "HELP" -> handleHelp(event, lang);
            case "RIZE" -> handleRize(event, lang);
            case "WEEKLY" -> handleWeeklyRanking(event, guildId, guild, lang);
            case "EVENT" -> handleEventRanking(event, guildId, guild, lang);
            case "MY_RANK" -> handleMyRank(event, guildId, authorId, author, lang);
            case "MONTHLY" -> handleMonthly(event, lang);
        }
    }

    // 도움말 처리
    private void handleHelp(MessageReceivedEvent event, String lang) {
        logger.info("도움말 요청을 받았습니다.");

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(MessageProvider.get(lang, "help.title"));
        eb.setColor(new Color(0x567ACE));
        eb.setDescription(MessageProvider.get(lang, "help.description"));

        eb.addField(MessageProvider.get(lang, "help.cmd.help"),
                MessageProvider.get(lang, "help.desc.help"), false);
        eb.addField(MessageProvider.get(lang, "help.cmd.rize"),
                MessageProvider.get(lang, "help.desc.rize"), false);
        eb.addField(MessageProvider.get(lang, "help.cmd.weekly"),
                MessageProvider.get(lang, "help.desc.weekly"), false);
        eb.addField(MessageProvider.get(lang, "help.cmd.event"),
                MessageProvider.get(lang, "help.desc.event"), false);
        eb.addField(MessageProvider.get(lang, "help.cmd.myrank"),
                MessageProvider.get(lang, "help.desc.myrank"), false);

        eb.setFooter(MessageProvider.get(lang, "help.footer"));

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    // 리제 오픈카톡 처리
    private void handleRize(MessageReceivedEvent event, String lang) {
        logger.info("리제쌤 문의 링크 요청을 받았습니다.");

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(MessageProvider.get(lang, "rize.title"), "https://open.kakao.com/o/sz17qsZf");
        eb.setColor(new Color(0xaca4e4));
        eb.setDescription(MessageProvider.get(lang, "rize.description"));
        eb.setFooter(MessageProvider.get(lang, "rize.footer"));

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    // 주간 랭킹 처리
    private void handleWeeklyRanking(MessageReceivedEvent event, String guildId, Guild guild, String lang) {
        logger.info("주간 랭킹 요청을 받음");
        List<RankingDto> weeklyRanking = rankingService.getWeeklyRanking(guildId);

        if (weeklyRanking.isEmpty()) {
            event.getChannel().sendMessage(MessageProvider.get(lang, "weekly.no_data")).queue();
            return;
        }

        StringBuilder rankMessage = new StringBuilder(MessageProvider.get(lang, "weekly.title"));

        for (int i = 0; i < weeklyRanking.size(); i++) {
            RankingDto ranker = weeklyRanking.get(i);
            String userName;

            try {
                Member member = guild.retrieveMemberById(ranker.getUserId()).complete();
                userName = member.getEffectiveName();
            } catch (Exception e) {
                userName = MessageProvider.get(lang, "weekly.user_not_found");
                logger.warn("{} ID를 가진 유저가 서버에 없어서 이름을 찾을 수 없습니다.", ranker.getUserId());
            }

            rankMessage.append(String.format("%d. %s - %s\n",
                    i + 1,
                    userName,
                    formatDuration(ranker.getTotalDuration(), lang)));
        }

        event.getChannel().sendMessage(rankMessage.toString()).queue();
    }

    // 이벤트 랭킹 처리
    private void handleEventRanking(MessageReceivedEvent event, String guildId, Guild guild, String lang) {
        logger.info("이벤트 랭킹 요청을 받았습니다.");

        if (!rankingService.isEventPeriod()) {
            event.getChannel().sendMessage(MessageProvider.get(lang, "event.not_period")).queue();
            return;
        }

        List<RankingDto> eventRanking = rankingService.getEventRanking(guildId);

        if (eventRanking.isEmpty()) {
            event.getChannel().sendMessage(MessageProvider.get(lang, "event.no_data")).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(MessageProvider.get(lang, "event.title"));
        eb.setColor(new Color(0xFF6B6B));

        StringBuilder description = new StringBuilder();
        description.append(MessageProvider.get(lang, "event.period"));

        for (int i = 0; i < eventRanking.size(); i++) {
            RankingDto ranker = eventRanking.get(i);
            String userName;

            try {
                Member member = guild.retrieveMemberById(ranker.getUserId()).complete();
                userName = member.getEffectiveName();
            } catch (Exception e) {
                userName = MessageProvider.get(lang, "weekly.user_not_found");
                logger.warn("{} ID를 가진 유저가 서버에 없어서 이름을 찾을 수 없습니다.", ranker.getUserId());
            }

            description.append(String.format("%d. %s - %s\n",
                    i + 1,
                    userName,
                    formatDuration(ranker.getTotalDuration(), lang)));
        }

        eb.setDescription(description.toString());
        eb.setFooter(MessageProvider.get(lang, "event.footer"));

        event.getChannel().sendMessageEmbeds(eb.build()).queue();
    }

    // 내 랭킹 처리
    private void handleMyRank(MessageReceivedEvent event, String guildId, String authorId, User author, String lang) {
        logger.info("{}님의 개인 정보 요청을 받았습니다.", author.getEffectiveName());

        // 1. 10위까지의 랭킹 데이터 가져오기
        List<RankingDto> weeklyRanking = rankingService.getWeeklyRanking(guildId);
        int myRank = -1;

        // 2. 10위 안에 내가 있는지 찾아보기
        for (int i = 0; i < weeklyRanking.size(); i++) {
            if (weeklyRanking.get(i).getUserId().equals(authorId)) {
                myRank = i + 1;
                break;
            }
        }

        StringBuilder dmMessage = new StringBuilder();
        dmMessage.append(MessageProvider.format(lang, "myrank.title", author.getEffectiveName()));

        if (myRank != -1) { // 10위 안에 내가 있을 경우
            long myTotalStudyTime = weeklyRanking.get(myRank - 1).getTotalDuration();
            dmMessage.append(MessageProvider.format(lang, "myrank.study_time",
                    formatDuration(myTotalStudyTime, lang)));
            dmMessage.append(MessageProvider.format(lang, "myrank.rank",
                    weeklyRanking.size(), myRank));

            if (myRank == 1) {
                dmMessage.append(MessageProvider.format(lang, "myrank.first", author.getEffectiveName()));
            } else {
                dmMessage.append(MessageProvider.get(lang, "myrank.encourage"));
            }
        } else { // 10위 안에 내가 없을 경우
            Optional<Long> optionalTotalTime = rankingService.getWeeklyTotalStudyTimeForUser(guildId, authorId);

            if (optionalTotalTime.isPresent() && optionalTotalTime.get() > 0) {
                dmMessage.append(MessageProvider.format(lang, "myrank.study_time",
                        formatDuration(optionalTotalTime.get(), lang)));
                dmMessage.append(MessageProvider.get(lang, "myrank.outside"));
                dmMessage.append(MessageProvider.get(lang, "myrank.outside_msg"));
            } else {
                dmMessage.append(MessageProvider.get(lang, "myrank.no_study"));
            }
        }

        // 4. DM으로 발송
        author.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(dmMessage.toString()).queue(
                    success -> event.getChannel().sendMessage(
                            MessageProvider.get(lang, "myrank.dm_sent")).queue(),
                    error -> {
                        logger.warn("{} 에게 DM 전송 실패, DM이 차단되었을 수 있습니다.", author.getName());
                        event.getChannel().sendMessage(
                                MessageProvider.get(lang, "myrank.dm_blocked")).queue();
                    });
        },
                error -> {
                    logger.warn("{} 의 개인 채널을 여는데 실패", author.getName());
                    event.getChannel().sendMessage(
                            MessageProvider.get(lang, "myrank.dm_failed")).queue();
                });
    }

    // 월간 랭킹 처리
    private void handleMonthly(MessageReceivedEvent event, String lang) {
        event.getChannel().sendMessage(MessageProvider.get(lang, "monthly.not_ready")).queue();
    }

    // 초를 "O시간 O분 O초" 또는 "O時間O分O秒" 형식으로 변환하는 메서드
    private String formatDuration(long totalSeconds, String lang) {
        if (totalSeconds < 60) {
            return MessageProvider.format(lang, "time.second", totalSeconds);
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return MessageProvider.format(lang, "time.hour", hours, minutes, seconds);
        } else {
            return MessageProvider.format(lang, "time.minute", minutes, seconds);
        }
    }
}
