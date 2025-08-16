package com.studybot.discord_study_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DiscordStudyBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscordStudyBotApplication.class, args);
	}

}
