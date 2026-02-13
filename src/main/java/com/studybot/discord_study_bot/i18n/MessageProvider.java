package com.studybot.discord_study_bot.i18n;

import java.util.Map;

public class MessageProvider {

    private static final Map<String, Map<String, String>> MESSAGES = Map.of(
            "ko", Map.ofEntries(
                    // 도움말
                    Map.entry("help.title", "**스터디 봇 도움말**"),
                    Map.entry("help.description", "음성 채널에서 공부 시간을 기록하고 랭킹을 보여주는 봇이에요! ✨"),
                    Map.entry("help.cmd.help", "/help"),
                    Map.entry("help.desc.help", "지금 보고 있는 이 도움말을 보여줘요."),
                    Map.entry("help.cmd.rize", "/lize"),
                    Map.entry("help.desc.rize", "리제쌤의 오픈카톡 링크를 보여줘요."),
                    Map.entry("help.cmd.weekly", "/weekly"),
                    Map.entry("help.desc.weekly", "이번 주의 공부 시간 랭킹을 보여줘요."),
                    Map.entry("help.cmd.event", "/event"),
                    Map.entry("help.desc.event", "이벤트 기간(10월~12월) 누계 공부 시간 랭킹을 보여줘요."),
                    Map.entry("help.cmd.myrank", "/myrank"),
                    Map.entry("help.desc.myrank", "나의 이번 주 공부 시간과 랭킹을 DM으로 알려줘요."),
                    Map.entry("help.footer", "열심히 공부하는 당신을 응원해요! 🔥"),

                    // 리제
                    Map.entry("rize.title", "💌 리제쌤에게 문의하기"),
                    Map.entry("rize.description", "리제쌤에게 과외문의 or 그밖의 문의/상담/질문 어느것이라도 좋아요!"),
                    Map.entry("rize.footer", "망설이지 말고 지금 바로 클릭! 👉"),

                    // 주간랭킹
                    Map.entry("weekly.title", "🏆 이번 주 공부 시간 랭킹 🏆"),
                    Map.entry("weekly.period", "이번 주 누적 공부 시간\n\n"),
                    Map.entry("weekly.no_data", "이번 주 공부 기록이 아직 없어요."),
                    Map.entry("weekly.user_not_found", "(서버에 없는 사용자)"),
                    Map.entry("weekly.footer", "매주 월요일 초기화! ⏰"),

                    // 이벤트
                    Map.entry("event.title", "🎉 이벤트 누계 공부 시간 랭킹 🎉"),
                    Map.entry("event.period", "이벤트 기간: 2025년 10월 1일 ~ 12월 31일\n\n"),
                    Map.entry("event.not_period", "현재 이벤트 기간이 아닙니다. (이벤트 기간: 2025년 10월 1일 ~ 12월 31일)"),
                    Map.entry("event.no_data", "이벤트 기간 공부 기록이 아직 없어요."),
                    Map.entry("event.footer", "이벤트 상품을 향해 달려봐요! 🍗🏃‍♂️💨"),

                    // 내랭킹
                    Map.entry("myrank.title", "**%s님의 공부 기록**"),
                    Map.entry("myrank.weekly_title", "📊 주간 공부시간"),
                    Map.entry("myrank.monthly_title", "📅 월간 공부시간"),
                    Map.entry("myrank.rank_suffix", "위"),
                    Map.entry("myrank.outside_rank", "10위권 밖"),
                    Map.entry("myrank.no_study_weekly", "이번 주 공부 기록이 없어요"),
                    Map.entry("myrank.no_study_monthly", "이번 달 공부 기록이 없어요"),
                    Map.entry("myrank.first", "🎉 누구보다 열심히 공부하는 %s님! 1위를 유지하세요!! 🎉"),
                    Map.entry("myrank.encourage", "좀더 달려서 1위를 노려봅시다!! 👍"),
                    Map.entry("myrank.footer", "꾸준히 공부하는 당신을 응원합니다! 💪"),
                    Map.entry("myrank.dm_sent", "개인 공부 기록이 DM으로 발송되었습니다."),
                    Map.entry("myrank.dm_blocked", "DM을 보내지 못했습니다. 혹시 DM을 차단한 건 아닌지 확인해 주세요"),
                    Map.entry("myrank.dm_failed", "DM을 보내는 데 실패했습니다. 잠시 후에 다시 시도해 주세요"),

                    // 월간랭킹
                    Map.entry("monthly.title", "📅 이번 달 공부 시간 랭킹 📅"),
                    Map.entry("monthly.period", "이번 달 누적 공부 시간\n\n"),
                    Map.entry("monthly.no_data", "이번 달 공부 기록이 아직 없어요."),
                    Map.entry("monthly.footer", "매달 1일에 초기화됩니다! 📆"),

                    // 시간 포맷
                    Map.entry("time.hour", "%d시간 %d분 %d초"),
                    Map.entry("time.minute", "%d분 %d초"),
                    Map.entry("time.second", "%d초")),

            "ja", Map.ofEntries(
                    // ヘルプ
                    Map.entry("help.title", "**スタディボット ヘルプ**"),
                    Map.entry("help.description", "ボイスチャンネルで勉強時間を記録し、ランキングを表示するボットです！✨"),
                    Map.entry("help.cmd.help", "/help"),
                    Map.entry("help.desc.help", "今見ているこのヘルプを表示します。"),
                    Map.entry("help.cmd.rize", "/lize"),
                    Map.entry("help.desc.rize", "リゼ先生のオープンチャットリンクを表示します。"),
                    Map.entry("help.cmd.weekly", "/weekly"),
                    Map.entry("help.desc.weekly", "今週の勉強時間ランキングを表示します。"),
                    Map.entry("help.cmd.event", "/event"),
                    Map.entry("help.desc.event", "イベント期間(10月~12月)累計勉強時間ランキングを表示します。"),
                    Map.entry("help.cmd.myrank", "/myrank"),
                    Map.entry("help.desc.myrank", "今週の自分の勉強時間とランキングをDMで教えます。"),
                    Map.entry("help.footer", "頑張って勉強するあなたを応援します！🔥"),

                    // リゼ
                    Map.entry("rize.title", "💌 リゼ先生に問い合わせる"),
                    Map.entry("rize.description", "リゼ先生に家庭教師のお問い合わせ、その他の相談や質問など、何でもお気軽にどうぞ！"),
                    Map.entry("rize.footer", "今すぐクリック！👉"),

                    // 週間ランキング
                    Map.entry("weekly.title", "🏆 今週の勉強時間ランキング 🏆"),
                    Map.entry("weekly.period", "今週の累計勉強時間\n\n"),
                    Map.entry("weekly.no_data", "今週の勉強記録はまだありません。"),
                    Map.entry("weekly.user_not_found", "(サーバーにいないユーザー)"),
                    Map.entry("weekly.footer", "毎週月曜日にリセット！⏰"),

                    // イベント
                    Map.entry("event.title", "🎉 イベント累計勉強時間ランキング 🎉"),
                    Map.entry("event.period", "イベント期間：2025年10月1日～12月31日\n\n"),
                    Map.entry("event.not_period", "現在イベント期間ではありません。（イベント期間：2025年10月1日～12月31日）"),
                    Map.entry("event.no_data", "イベント期間の勉強記録はまだありません。"),
                    Map.entry("event.footer", "イベント商品を目指して頑張ろう！🍗🏃‍♂️💨"),

                    // マイランキング
                    Map.entry("myrank.title", "**%sさんの勉強記録**"),
                    Map.entry("myrank.weekly_title", "📊 週間勉強時間"),
                    Map.entry("myrank.monthly_title", "📅 月間勉強時間"),
                    Map.entry("myrank.rank_suffix", "位"),
                    Map.entry("myrank.outside_rank", "10位圏外"),
                    Map.entry("myrank.no_study_weekly", "今週の勉強記録がありません"),
                    Map.entry("myrank.no_study_monthly", "今月の勉強記録がありません"),
                    Map.entry("myrank.first", "🎉 誰よりも頑張って勉強する%sさん！1位を維持してください！！🎉"),
                    Map.entry("myrank.encourage", "もう少し頑張って1位を目指しましょう！！👍"),
                    Map.entry("myrank.footer", "頑張るあなたを応援します！💪"),
                    Map.entry("myrank.dm_sent", "個人の勉強記録がDMで送信されました。"),
                    Map.entry("myrank.dm_blocked", "DMを送信できませんでした。DMをブロックしていないか確認してください"),
                    Map.entry("myrank.dm_failed", "DMの送信に失敗しました。しばらくしてからもう一度お試しください"),

                    // 月間ランキング
                    Map.entry("monthly.title", "📅 今月の勉強時間ランキング 📅"),
                    Map.entry("monthly.period", "今月の累計勉強時間\n\n"),
                    Map.entry("monthly.no_data", "今月の勉強記録はまだありません。"),
                    Map.entry("monthly.footer", "毎月1日にリセットされます！📆"),

                    // 時間フォーマット
                    Map.entry("time.hour", "%d時間%d分%d秒"),
                    Map.entry("time.minute", "%d分%d秒"),
                    Map.entry("time.second", "%d秒")));

    /**
     * 언어 코드와 키로 메시지를 가져옴
     */
    public static String get(String lang, String key) {
        return MESSAGES.getOrDefault(lang, MESSAGES.get("ko")).getOrDefault(key, key);
    }

    /**
     * 언어 코드와 키로 메시지를 가져와서 포맷팅
     */
    public static String format(String lang, String key, Object... args) {
        String template = get(lang, key);
        return String.format(template, args);
    }
}
