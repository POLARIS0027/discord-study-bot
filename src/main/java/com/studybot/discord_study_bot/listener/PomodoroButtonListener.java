package com.studybot.discord_study_bot.listener;

import com.studybot.discord_study_bot.i18n.MessageProvider;
import com.studybot.discord_study_bot.pomodoro.PomodoroState;
import com.studybot.discord_study_bot.pomodoro.SharedPomodoroSession;
import com.studybot.discord_study_bot.service.SharedPomodoroService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 뽀모도로 타이머 버튼 인터랙션 처리
 */
@Component
@RequiredArgsConstructor
public class PomodoroButtonListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PomodoroButtonListener.class);
    private final SharedPomodoroService sharedPomodoroService;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        User user = event.getUser();
        String userId = user.getId();
        String userName = user.getName();
        Guild guild = event.getGuild();

        // 길드에서만 실행
        if (guild == null) {
            event.reply("이 기능은 서버에서만 사용할 수 있습니다.").setEphemeral(true).queue();
            return;
        }

        String guildName = guild.getName();
        
        // 언어 감지
        String lang = event.getUserLocale().getLocale().startsWith("ko") ? "ko" : "ja";

        // 공유 뽀모도로 버튼 처리
        if (buttonId.startsWith("shared_join_")) {
            String channelId = buttonId.substring("shared_join_".length());
            sharedPomodoroService.addParticipant(channelId, userId, userName, guildName, lang);
            event.reply(MessageProvider.get(lang, "shared.joined")).setEphemeral(true).queue();
            
        } else if (buttonId.startsWith("shared_leave_")) {
            String channelId = buttonId.substring("shared_leave_".length());
            sharedPomodoroService.removeParticipant(channelId, userId, userName, lang);
            event.reply(MessageProvider.get(lang, "shared.left")).setEphemeral(true).queue();
            
        } else if (buttonId.startsWith("shared_stop_")) {
            String channelId = buttonId.substring("shared_stop_".length());
            sharedPomodoroService.stopSharedTimer(channelId);
            event.reply(MessageProvider.get(lang, "pomodoro.stopped")).setEphemeral(true).queue();
            
        } else if (buttonId.startsWith("shared_pause_")) {
            String channelId = buttonId.substring("shared_pause_".length());
            sharedPomodoroService.pauseTimer(channelId, lang);
            event.reply(MessageProvider.get(lang, "pomodoro.paused_msg")).setEphemeral(true).queue();
            
        } else if (buttonId.startsWith("shared_resume_")) {
            String channelId = buttonId.substring("shared_resume_".length());
            SharedPomodoroSession session = sharedPomodoroService.getActiveSession(channelId);
            if (session != null) {
                PomodoroState previousState = session.getRemainingSeconds() > 0 ? PomodoroState.STUDY : PomodoroState.SHORT_BREAK;
                sharedPomodoroService.resumeTimer(channelId, previousState, lang);
                event.reply(MessageProvider.get(lang, "pomodoro.resumed")).setEphemeral(true).queue();
            }
            
        } else if (buttonId.equals("shared_ignore")) {
            event.reply(MessageProvider.get(lang, "pomodoro.ignored")).setEphemeral(true).queue();
        }
        
        logger.info("[{}] {}님이 버튼 클릭: {}", guildName, userName, buttonId);
    }
}
