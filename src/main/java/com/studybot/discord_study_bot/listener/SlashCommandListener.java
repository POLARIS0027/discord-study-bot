package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.i18n.MessageProvider;
import com.studybot.discord_study_bot.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Discord Slash Commands를 처리하는 리스너
 */
@Component
@RequiredArgsConstructor
public class SlashCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SlashCommandListener.class);
    private final RankingService rankingService;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // 봇이 보낸 명령어는 무시 (안전장치)
        if (event.getUser().isBot()) {
            return;
        }

        // 길드(서버)에서만 실행 가능
        if (!event.isFromGuild() || event.getGuild() == null) {
            event.reply("이 명령어는 서버에서만 사용할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        String commandName = event.getName();
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        User author = event.getUser();
        String authorId = author.getId();

        // 사용자 언어 감지 (Discord 설정 기반)
        String lang = detectLanguage(event);

        logger.info("[{}] /{} 명령어 요청을 받았습니다. (사용자: {}, 언어: {})",
                guild.getName(), commandName, author.getName(), lang);

        // 명령어별 처리
        switch (commandName) {
            case "help" -> handleHelp(event, lang);
            case "lize" -> handleLize(event, lang);
            case "weekly" -> handleWeeklyRanking(event, guildId, guild, lang);
            case "event" -> handleEventRanking(event, guildId, guild, lang);
            case "myrank" -> handleMyRank(event, guildId, authorId, author, lang);
            case "monthly" -> handleMonthly(event, lang);
            default -> event.reply("알 수 없는 명령어입니다.").setEphemeral(true).queue();
        }
    }

    /**
     * 사용자의 Discord 언어 설정을 감지하여 ko 또는 ja 반환
     */
    private String detectLanguage(SlashCommandInteractionEvent event) {
        String locale = event.getUserLocale().getLocale();
        
        // 한국어면 "ko", 일본어면 "ja", 그 외는 기본값 "ko"
        if (locale.startsWith("ko")) {
            return "ko";
        } else if (locale.startsWith("ja")) {
            return "ja";
        } else {
            return "ko"; // 기본값
        }
    }

    // 도움말 처리
    private void handleHelp(SlashCommandInteractionEvent event, String lang) {
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

        event.replyEmbeds(eb.build()).queue();
    }

    // 리제 오픈카톡 처리
    private void handleLize(SlashCommandInteractionEvent event, String lang) {
        logger.info("리제쌤 문의 링크 요청을 받았습니다.");

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(MessageProvider.get(lang, "rize.title"), "https://open.kakao.com/o/sz17qsZf");
        eb.setColor(new Color(0xaca4e4));
        eb.setDescription(MessageProvider.get(lang, "rize.description"));
        eb.setFooter(MessageProvider.get(lang, "rize.footer"));

        event.replyEmbeds(eb.build()).queue();
    }

    // 주간 랭킹 처리
    private void handleWeeklyRanking(SlashCommandInteractionEvent event, String guildId, Guild guild, String lang) {
        logger.info("주간 랭킹 요청을 받음");
        
        // 처리 시간이 3초 이상 걸릴 수 있으므로 deferReply 사용
        event.deferReply().queue();

        List<RankingDto> weeklyRanking = rankingService.getWeeklyRanking(guildId);

        if (weeklyRanking.isEmpty()) {
            event.getHook().sendMessage(MessageProvider.get(lang, "weekly.no_data")).queue();
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

        event.getHook().sendMessage(rankMessage.toString()).queue();
    }

    // 이벤트 랭킹 처리
    private void handleEventRanking(SlashCommandInteractionEvent event, String guildId, Guild guild, String lang) {
        logger.info("이벤트 랭킹 요청을 받았습니다.");

        if (!rankingService.isEventPeriod()) {
            event.reply(MessageProvider.get(lang, "event.not_period")).queue();
            return;
        }

        // 처리 시간이 3초 이상 걸릴 수 있으므로 deferReply 사용
        event.deferReply().queue();

        List<RankingDto> eventRanking = rankingService.getEventRanking(guildId);

        if (eventRanking.isEmpty()) {
            event.getHook().sendMessage(MessageProvider.get(lang, "event.no_data")).queue();
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

        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }

    // 내 랭킹 처리
    private void handleMyRank(SlashCommandInteractionEvent event, String guildId, String authorId, User author, String lang) {
        logger.info("{}님의 개인 정보 요청을 받았습니다.", author.getName());

        // 처리 시간이 3초 이상 걸릴 수 있으므로 deferReply 사용
        event.deferReply().setEphemeral(true).queue(); // ephemeral로 나만 보이게 설정

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
        dmMessage.append(MessageProvider.format(lang, "myrank.title", author.getName()));

        if (myRank != -1) { // 10위 안에 내가 있을 경우
            long myTotalStudyTime = weeklyRanking.get(myRank - 1).getTotalDuration();
            dmMessage.append(MessageProvider.format(lang, "myrank.study_time",
                    formatDuration(myTotalStudyTime, lang)));
            dmMessage.append(MessageProvider.format(lang, "myrank.rank",
                    weeklyRanking.size(), myRank));

            if (myRank == 1) {
                dmMessage.append(MessageProvider.format(lang, "myrank.first", author.getName()));
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
                    success -> event.getHook().sendMessage(
                            MessageProvider.get(lang, "myrank.dm_sent")).setEphemeral(true).queue(),
                    error -> {
                        logger.warn("{} 에게 DM 전송 실패, DM이 차단되었을 수 있습니다.", author.getName());
                        event.getHook().sendMessage(
                                MessageProvider.get(lang, "myrank.dm_blocked")).setEphemeral(true).queue();
                    });
        },
                error -> {
                    logger.warn("{} 의 개인 채널을 여는데 실패", author.getName());
                    event.getHook().sendMessage(
                            MessageProvider.get(lang, "myrank.dm_failed")).setEphemeral(true).queue();
                });
    }

    // 월간 랭킹 처리
    private void handleMonthly(SlashCommandInteractionEvent event, String lang) {
        event.reply(MessageProvider.get(lang, "monthly.not_ready")).queue();
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
