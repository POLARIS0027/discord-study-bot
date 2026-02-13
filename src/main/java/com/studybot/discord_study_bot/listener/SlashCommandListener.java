package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.i18n.MessageProvider;
import com.studybot.discord_study_bot.service.RankingService;
import com.studybot.discord_study_bot.service.SharedPomodoroService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
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
    private final SharedPomodoroService sharedPomodoroService;

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
            case "monthly" -> handleMonthly(event, guildId, guild, lang);
            case "pomodoro-shared" -> handleSharedPomodoro(event, guildId, author, lang);
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

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(MessageProvider.get(lang, "weekly.title"));
        eb.setColor(new Color(0x5865F2)); // Discord Blurple

        StringBuilder description = new StringBuilder();
        description.append(MessageProvider.get(lang, "weekly.period"));

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

            description.append(String.format("%d. %s - %s\n",
                    i + 1,
                    userName,
                    formatDuration(ranker.getTotalDuration(), lang)));
        }

        eb.setDescription(description.toString());
        eb.setFooter(MessageProvider.get(lang, "weekly.footer"));

        event.getHook().sendMessageEmbeds(eb.build()).queue();
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
        event.deferReply().setEphemeral(true).queue();

        // 1. 주간 랭킹 조회
        List<RankingDto> weeklyRanking = rankingService.getWeeklyRanking(guildId);
        int myWeeklyRank = -1;

        for (int i = 0; i < weeklyRanking.size(); i++) {
            if (weeklyRanking.get(i).getUserId().equals(authorId)) {
                myWeeklyRank = i + 1;
                break;
            }
        }

        // 2. 개인 주간/월간 공부시간 조회
        Optional<Long> weeklyTime = rankingService.getWeeklyTotalStudyTimeForUser(guildId, authorId);
        Optional<Long> monthlyTime = rankingService.getMonthlyTotalStudyTimeForUser(guildId, authorId);

        // 3. Embed 메시지 구성
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(MessageProvider.format(lang, "myrank.title", author.getName()));
        eb.setColor(new Color(0xFEE75C)); // Discord Yellow
        
        // 프로필 사진 추가
        String avatarUrl = author.getAvatarUrl();
        if (avatarUrl != null) {
            eb.setThumbnail(avatarUrl);
        }

        // 주간 공부시간
        if (weeklyTime.isPresent() && weeklyTime.get() > 0) {
            String weeklyDuration = formatDuration(weeklyTime.get(), lang);
            String weeklyRankText = myWeeklyRank != -1 
                ? String.format("%d/%d%s", myWeeklyRank, weeklyRanking.size(), 
                    MessageProvider.get(lang, "myrank.rank_suffix"))
                : MessageProvider.get(lang, "myrank.outside_rank");
            
            eb.addField(
                MessageProvider.get(lang, "myrank.weekly_title"),
                String.format("⏱️ %s\n🏆 %s", weeklyDuration, weeklyRankText),
                false
            );
        } else {
            eb.addField(
                MessageProvider.get(lang, "myrank.weekly_title"),
                MessageProvider.get(lang, "myrank.no_study_weekly"),
                false
            );
        }

        // 월간 공부시간
        if (monthlyTime.isPresent() && monthlyTime.get() > 0) {
            String monthlyDuration = formatDuration(monthlyTime.get(), lang);
            eb.addField(
                MessageProvider.get(lang, "myrank.monthly_title"),
                String.format("⏱️ %s", monthlyDuration),
                false
            );
        } else {
            eb.addField(
                MessageProvider.get(lang, "myrank.monthly_title"),
                MessageProvider.get(lang, "myrank.no_study_monthly"),
                false
            );
        }

        // 격려 메시지
        if (myWeeklyRank == 1) {
            eb.setDescription(MessageProvider.format(lang, "myrank.first", author.getName()));
        } else if (weeklyTime.isPresent() && weeklyTime.get() > 0) {
            eb.setDescription(MessageProvider.get(lang, "myrank.encourage"));
        }

        eb.setFooter(MessageProvider.get(lang, "myrank.footer"));
        eb.setTimestamp(java.time.Instant.now());

        // 4. DM으로 발송
        author.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessageEmbeds(eb.build()).queue(
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
    private void handleMonthly(SlashCommandInteractionEvent event, String guildId, Guild guild, String lang) {
        logger.info("월간 랭킹 요청을 받았습니다.");

        // 처리 시간이 3초 이상 걸릴 수 있으므로 deferReply 사용
        event.deferReply().queue();

        List<RankingDto> monthlyRanking = rankingService.getMonthlyRanking(guildId);

        if (monthlyRanking.isEmpty()) {
            event.getHook().sendMessage(MessageProvider.get(lang, "monthly.no_data")).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(MessageProvider.get(lang, "monthly.title"));
        eb.setColor(new Color(0x57F287)); // Discord Green

        StringBuilder description = new StringBuilder();
        description.append(MessageProvider.get(lang, "monthly.period"));

        for (int i = 0; i < monthlyRanking.size(); i++) {
            RankingDto ranker = monthlyRanking.get(i);
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
        eb.setFooter(MessageProvider.get(lang, "monthly.footer"));

        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }

    // 공유 뽀모도로 처리
    private void handleSharedPomodoro(SlashCommandInteractionEvent event, String guildId, User author, String lang) {
        logger.info("공유 뽀모도로 시작 요청을 받았습니다.");

        // 음성 채널에 연결되어 있는지 확인
        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
            event.reply(MessageProvider.get(lang, "pomodoro.not_in_voice")).setEphemeral(true).queue();
            return;
        }

        if (member.getVoiceState().getChannel() == null) {
            event.reply(MessageProvider.get(lang, "pomodoro.not_in_voice")).setEphemeral(true).queue();
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        String voiceChannelId = voiceChannel.getId();
        String textChannelId = event.getChannel().getId();

        // 옵션 파싱
        OptionMapping studyOption = event.getOption("study");
        OptionMapping breakOption = event.getOption("break");
        OptionMapping autoStartOption = event.getOption("autostart");

        int studyMinutes = studyOption != null ? studyOption.getAsInt() : 25;
        int breakMinutes = breakOption != null ? breakOption.getAsInt() : 5;
        boolean autoStart = autoStartOption != null && autoStartOption.getAsBoolean();

        // 입력값 검증
        if (studyMinutes < 1 || studyMinutes > 120) {
            event.reply(MessageProvider.get(lang, "pomodoro.invalid_study_time")).setEphemeral(true).queue();
            return;
        }
        if (breakMinutes < 1 || breakMinutes > 30) {
            event.reply(MessageProvider.get(lang, "pomodoro.invalid_break_time")).setEphemeral(true).queue();
            return;
        }

        // 공유 타이머 시작
        sharedPomodoroService.startSharedTimer(voiceChannelId, guildId, textChannelId, 
                studyMinutes, breakMinutes, autoStart, lang);

        event.reply(MessageProvider.format(lang, "pomodoro.shared_started", 
                voiceChannel.getName(), studyMinutes, breakMinutes))
                .setEphemeral(true)
                .queue();
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
