package com.studybot.discord_study_bot.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Discord Slash Commands를 등록하는 설정 클래스
 * 봇 시작 시 자동으로 모든 명령어를 Discord API에 등록합니다.
 */
@Component
@RequiredArgsConstructor
public class SlashCommandRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(SlashCommandRegistrar.class);
    private final JDA jda;

    /**
     * 봇 시작 시 Slash Commands를 Discord API에 등록
     */
    @PostConstruct
    public void registerCommands() {
        logger.info("Slash Commands 등록을 시작합니다...");

        List<CommandData> commands = new ArrayList<>();

        // 1. /help - 도움말
        commands.add(Commands.slash("help", "Show bot help and available commands")
                .setNameLocalizations(Map.of(
                        DiscordLocale.KOREAN, "도움말",
                        DiscordLocale.JAPANESE, "ヘルプ"
                ))
                .setDescriptionLocalizations(Map.of(
                        DiscordLocale.KOREAN, "봇 도움말과 사용 가능한 명령어를 표시합니다",
                        DiscordLocale.JAPANESE, "ボットのヘルプと利用可能なコマンドを表示します"
                ))
        );

        // 2. /lize - 리제쌤 오픈카톡
        commands.add(Commands.slash("lize", "Show Lize's open chat link")
                .setNameLocalizations(Map.of(
                        DiscordLocale.KOREAN, "리제",
                        DiscordLocale.JAPANESE, "リゼ"
                ))
                .setDescriptionLocalizations(Map.of(
                        DiscordLocale.KOREAN, "리제쌤의 오픈카톡 링크를 표시합니다",
                        DiscordLocale.JAPANESE, "リゼ先生のオープンチャットリンクを表示します"
                ))
        );

        // 3. /weekly - 주간 랭킹
        commands.add(Commands.slash("weekly", "Show weekly study time ranking")
                .setNameLocalizations(Map.of(
                        DiscordLocale.KOREAN, "주간랭킹",
                        DiscordLocale.JAPANESE, "週間ランキング"
                ))
                .setDescriptionLocalizations(Map.of(
                        DiscordLocale.KOREAN, "이번 주의 공부 시간 랭킹을 표시합니다",
                        DiscordLocale.JAPANESE, "今週の勉強時間ランキングを表示します"
                ))
        );

        // 4. /event - 이벤트 랭킹
        commands.add(Commands.slash("event", "Show event period cumulative ranking")
                .setNameLocalizations(Map.of(
                        DiscordLocale.KOREAN, "이벤트",
                        DiscordLocale.JAPANESE, "イベント"
                ))
                .setDescriptionLocalizations(Map.of(
                        DiscordLocale.KOREAN, "이벤트 기간 누계 공부 시간 랭킹을 표시합니다",
                        DiscordLocale.JAPANESE, "イベント期間累計勉強時間ランキングを表示します"
                ))
        );

        // 5. /myrank - 내 랭킹
        commands.add(Commands.slash("myrank", "Check your study time and ranking via DM")
                .setNameLocalizations(Map.of(
                        DiscordLocale.KOREAN, "내랭킹",
                        DiscordLocale.JAPANESE, "マイランキング"
                ))
                .setDescriptionLocalizations(Map.of(
                        DiscordLocale.KOREAN, "나의 공부 시간과 랭킹을 DM으로 확인합니다",
                        DiscordLocale.JAPANESE, "自分の勉強時間とランキングをDMで確認します"
                ))
        );

        // 6. /monthly - 월간 랭킹 (준비 중)
        commands.add(Commands.slash("monthly", "Show monthly study time ranking (coming soon)")
                .setNameLocalizations(Map.of(
                        DiscordLocale.KOREAN, "월간랭킹",
                        DiscordLocale.JAPANESE, "月間ランキング"
                ))
                .setDescriptionLocalizations(Map.of(
                        DiscordLocale.KOREAN, "월간 공부 시간 랭킹을 표시합니다 (준비 중)",
                        DiscordLocale.JAPANESE, "月間勉強時間ランキングを表示します（準備中）"
                ))
        );

        // Discord API에 명령어 등록 (글로벌 명령어)
        jda.updateCommands().addCommands(commands).queue(
                success -> logger.info("{} 개의 Slash Commands가 성공적으로 등록되었습니다.", commands.size()),
                error -> logger.error("Slash Commands 등록 중 오류 발생", error)
        );

        logger.info("Slash Commands 등록이 완료되었습니다. (최대 1시간 소요될 수 있습니다)");
    }
}
