package com.studybot.discord_study_bot.config;

import java.util.Map;

public class CommandConfig {

    // 커맨드명 → 언어 매핑
    public static final Map<String, String> COMMAND_LANG = Map.ofEntries(
            Map.entry("도움말", "ko"),
            Map.entry("ヘルプ", "ja"),
            Map.entry("주간랭킹", "ko"),
            Map.entry("週間ランキング", "ja"),
            Map.entry("내랭킹", "ko"),
            Map.entry("マイランキング", "ja"),
            Map.entry("이벤트", "ko"),
            Map.entry("イベント", "ja"),
            Map.entry("리제", "ko"),
            Map.entry("リゼ", "ja"),
            Map.entry("월간랭킹", "ko"),
            Map.entry("月間ランキング", "ja"));

    // 커맨드명 → 액션 타입 매핑
    public static final Map<String, String> COMMAND_ACTION = Map.ofEntries(
            Map.entry("도움말", "HELP"),
            Map.entry("ヘルプ", "HELP"),
            Map.entry("주간랭킹", "WEEKLY"),
            Map.entry("週間ランキング", "WEEKLY"),
            Map.entry("내랭킹", "MY_RANK"),
            Map.entry("マイランキング", "MY_RANK"),
            Map.entry("이벤트", "EVENT"),
            Map.entry("イベント", "EVENT"),
            Map.entry("리제", "RIZE"),
            Map.entry("リゼ", "RIZE"),
            Map.entry("월간랭킹", "MONTHLY"),
            Map.entry("月間ランキング", "MONTHLY"));

    /**
     * 커맨드로부터 언어 코드를 가져옴
     */
    public static String getLanguage(String command) {
        return COMMAND_LANG.getOrDefault(command, "ko");
    }

    /**
     * 커맨드로부터 액션 타입을 가져옴
     */
    public static String getAction(String command) {
        return COMMAND_ACTION.get(command);
    }
}
