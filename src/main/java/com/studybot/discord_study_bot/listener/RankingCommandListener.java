package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.dto.RankingDto;
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

    // application.yml에서 discord.prefix 값을 가져와서 할당
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
        String authorId = author.getId(); // 닉네임 대신 ID를 사용

        // prefix로 시작하지 않으면 무시함
        if (!message.startsWith(prefix)) {
            return;
        }

        // 명령어 추출
        String command = message.substring(prefix.length());

        switch (command) {
            case "도움말" -> { // !도움말 명령어 처리
                logger.info("도움말 요청을 받았습니다.");

                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("**스터디 봇 도움말**");
                eb.setColor(new Color(0x567ACE));
                eb.setDescription("음성 채널에서 공부 시간을 기록하고 랭킹을 보여주는 봇이에요! ✨");

                eb.addField("!도움말", "지금 보고 있는 이 도움말을 보여줘요.", false);
                eb.addField("!리제", "리제쌤의 오픈카톡 링크를 보여줘요.", false);
                eb.addField("!주간랭킹", "이번 주의 공부 시간 랭킹을 보여줘요.", false);
                eb.addField("!이벤트", "이벤트 기간(10월~12월) 누계 공부 시간 랭킹을 보여줘요.", false);
                eb.addField("!내랭킹", "나의 이번 주 공부 시간과 랭킹을 DM으로 알려줘요.", false);

                eb.setFooter("열심히 공부하는 당신을 응원해요! 🔥");

                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            }
            case "리제" -> { // 리제 오픈카톡 표시
                logger.info("리제쌤 문의 링크 요청을 받았습니다.");

                EmbedBuilder eb = new EmbedBuilder();
                // 제목을 클릭하면 링크로 이동
                eb.setTitle("💌 리제쌤에게 문의하기", "https://open.kakao.com/o/sz17qsZf");
                eb.setColor(new Color(0xaca4e4));
                eb.setDescription("리제쌤에게 과외문의 or 그밖의 문의/상담/질문 어느것이라도 좋아요!");
                eb.setFooter("망설이지 말고 지금 바로 클릭! 👉");

                event.getChannel().sendMessageEmbeds(eb.build()).queue();

            }
            case "주간랭킹" -> {
                logger.info("주간 랭킹 요청을 받음");
                List<RankingDto> weeklyRanking = rankingService.getWeeklyRanking();

                if (weeklyRanking.isEmpty()) {
                    event.getChannel().sendMessage("이번 주 공부 기록이 아직 없어요.").queue();
                    return;
                }

                // DESC정렬로 DB에서 받아오니까, 순서대로 순회하면서 추가한다. 랭킹을 몇위까지 표시할지는 상담
                Guild guild = event.getGuild();
                StringBuilder rankMessage = new StringBuilder("🏆 이번 주 공부 시간 랭킹 🏆\n");

                for (int i = 0; i < weeklyRanking.size(); i++) {
                    RankingDto ranker = weeklyRanking.get(i);
                    String userName;

                    try {
                        // userId로 서버에서 멤버 정보를 가져옴
                        Member member = guild.retrieveMemberById(ranker.getUserId()).complete();
                        // 멤버의 서버 별명을 가져옴
                        userName = member.getEffectiveName();
                    } catch (Exception e) {
                        // 유저가 서버에 없는 경우
                        userName = "(서버에 없는 사용자)";
                        logger.warn("{} ID를 가진 유저가 서버에 없어서 이름을 찾을 수 없습니다.", ranker.getUserId());
                    }

                    rankMessage.append(String.format("%d. %s - %s\n",
                            i + 1,
                            userName, // id를 이름으로 교체함
                            formatDuration(ranker.getTotalDuration())));
                }

                event.getChannel().sendMessage(rankMessage.toString()).queue();
            }
            case "월간랭킹" ->
                // Todo:월간 랭킹 로직 작성
                event.getChannel().sendMessage("월간 랭킹 기능은 준비중입니다").queue();
            case "이벤트" -> {
                logger.info("이벤트 랭킹 요청을 받았습니다.");

                // 이벤트 기간 체크
                if (!rankingService.isEventPeriod()) {
                    event.getChannel().sendMessage("현재 이벤트 기간이 아닙니다. (이벤트 기간: 2025년 10월 1일 ~ 12월 31일)").queue();
                    return;
                }

                List<RankingDto> eventRanking = rankingService.getEventRanking();

                if (eventRanking.isEmpty()) {
                    event.getChannel().sendMessage("이벤트 기간 공부 기록이 아직 없어요.").queue();
                    return;
                }

                // 이벤트 랭킹 메시지 생성
                Guild guild = event.getGuild();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("🎉 이벤트 누계 공부 시간 랭킹 🎉");
                eb.setColor(new Color(0xFF6B6B)); // 빨간색!

                StringBuilder description = new StringBuilder();
                description.append("이벤트 기간: 2025년 10월 1일 ~ 12월 31일\n\n");

                for (int i = 0; i < eventRanking.size(); i++) {
                    RankingDto ranker = eventRanking.get(i);
                    String userName;

                    try {
                        // userId로 서버에서 멤버 정보를 가져옴
                        Member member = guild.retrieveMemberById(ranker.getUserId()).complete();
                        // 멤버의 서버 별명을 가져옴
                        userName = member.getEffectiveName();
                    } catch (Exception e) {
                        // 유저가 서버에 없는 경우
                        userName = "(서버에 없는 사용자)";
                        logger.warn("{} ID를 가진 유저가 서버에 없어서 이름을 찾을 수 없습니다.", ranker.getUserId());
                    }

                    description.append(String.format("%d. %s - %s\n",
                            i + 1,
                            userName,
                            formatDuration(ranker.getTotalDuration())));
                }

                eb.setDescription(description.toString());
                eb.setFooter("이벤트 상품을 향해 달려봐요! 🍗🏃‍♂️💨");

                event.getChannel().sendMessageEmbeds(eb.build()).queue();
            }
            case "내랭킹" -> {
                logger.info("{}님의 개인 정보 요청을 받았습니다.", author.getEffectiveName());

                // 1. 10위까지의 랭킹 데이터 가져오기
                List<RankingDto> weeklyRanking = rankingService.getWeeklyRanking();
                int myRank = -1;

                // 2. 10위 안에 내가 있는지 찾아보기
                for (int i = 0; i < weeklyRanking.size(); i++) {
                    if (weeklyRanking.get(i).getUserId().equals(authorId)) {
                        myRank = i + 1;
                        break;
                    }
                }

                StringBuilder dmMessage = new StringBuilder();
                dmMessage.append(String.format(" **%s님의 공부 기록**\n\n", author.getEffectiveName()));

                if (myRank != -1) { // 10위 안에 내가 있을 경우
                    long myTotalStudyTime = weeklyRanking.get(myRank - 1).getTotalDuration();
                    dmMessage.append(String.format("✍️ 이번주 공부시간: **%s**\n\n", formatDuration(myTotalStudyTime)));
                    dmMessage.append(String.format("현재 %d명중 🏆 **%d위** 입니다.\n\n", weeklyRanking.size(), myRank));

                    if (myRank == 1) {
                        dmMessage.append(
                                String.format("🎉 누구보다 열심히 공부하는 %s! 1위를 유지하세요!! 🎉", author.getEffectiveName()));
                    } else {
                        dmMessage.append("좀더 달려서 1위를 노려봅시다!! 👍");
                    }
                } else { // 10위 안에 내가 없을 경우
                    // 공부시간이 있는데 랭킹엔 없거나 공부시간이 존재하지 않음
                    Optional<Long> optionalTotalTime = rankingService.getWeeklyTotalStudyTimeForUser(authorId);

                    if (optionalTotalTime.isPresent() && optionalTotalTime.get() > 0) { // 랭킹엔 없지만 공부 기록은 있을 때
                        dmMessage.append(
                                String.format("✍️ 이번주 공부시간: **%s**\n\n", formatDuration(optionalTotalTime.get())));
                        dmMessage.append("🏆 현재 랭킹: **10위권 밖** 입니다.\n\n");
                        dmMessage.append("아쉽지만 순위권 밖이라도 괜찮아! 꾸준히 하는 게 제일 중요해! 💪");
                    } else { // 정말로 공부 기록이 없을 때
                        dmMessage.append("이번주엔 공부 안하는구나? 👍\n\n");
                        dmMessage.append("다음주에도 안할예정이니?\n");
                        dmMessage.append("오늘부터라도 ㄱㄱ 😎");
                    }
                }

                // 4. DM으로 발송
                author.openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage(dmMessage.toString()).queue(
                            // 메세지 전송 성공시
                            success -> event.getChannel().sendMessage("개인 공부 기록이 DM으로 발송되었습니다.").queue(),
                            // 메세지 전송 실패시
                            error -> {
                                logger.warn("{} 에게 DM 전송 실패, DM이 차단되었을 수 있습니다.", author.getName());
                                event.getChannel().sendMessage("DM을 보내지 못했습니다. 혹시 DM을 차단한 건 아닌지 확인해 주세요").queue();
                            });
                },
                        // 개인 채널 오픈 실패시
                        error -> {
                            logger.warn("{} 의 개인 채널을 여는데 실패", author.getName());
                            event.getChannel().sendMessage("DM을 보내는 데 실패했습니다. 잠시 후에 다시 시도해 주세요").queue();
                        });
            }
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
