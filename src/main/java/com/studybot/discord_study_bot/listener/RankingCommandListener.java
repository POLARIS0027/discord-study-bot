package com.studybot.discord_study_bot.listener;


import com.studybot.discord_study_bot.dto.RankingDto;
import com.studybot.discord_study_bot.service.RankingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RankingCommandListener.class);
    private final RankingService rankingService;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 봇이 보낸 메세지나 서버에서 온 메세지가 아니면 무시
        if (event.getAuthor().isBot() || !event.isFromGuild()) {
            return;
        }

        String message = event.getMessage().getContentRaw();

        if (message.equals("!주간랭킹")) {
            logger.info("주간 랭킹 요청을 받음");
            List<RankingDto> weeklyRanking = rankingService.getWeeklyRanking();

            if (weeklyRanking.isEmpty()) {
                event.getChannel().sendMessage("이번 주 공부 기록이 아직 없어요.").queue();
                return;
            }

            // DESC정렬로 DB에서 받아오니까, 순서대로 순회하면서 추가한다. 랭킹을 몇위까지 표시할지는 상담
            StringBuilder rankMessage = new StringBuilder("🏆 이번 주 공부 시간 랭킹 🏆\n");
            for (int i = 0; i < weeklyRanking.size(); i++) {
                RankingDto ranker = weeklyRanking.get(i);
                rankMessage.append(String.format("%d. %s - %s\n",
                        i + 1,
                        ranker.getUserName(),
                        formatDuration(ranker.getTotalDuration())));
            }

            event.getChannel().sendMessage(rankMessage.toString()).queue();
        } else if (message.equals("!월간랭킹")) {
            // Todo:월간 랭킹 로직 작성
            event.getChannel().sendMessage("월간 랭킹 기능은 준비중입니다").queue();
        }
    }

    // 초를 "O시간 O분 O초" 형식으로 변환하는 메서드
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
