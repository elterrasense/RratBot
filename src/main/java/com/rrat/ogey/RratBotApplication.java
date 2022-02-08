package com.rrat.ogey;

import com.rrat.ogey.listeners.CommandDispatcherListener;
import com.rrat.ogey.listeners.KeywordListener;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class RratBotApplication {

	//Set token as environmental variable
	@Autowired
	private Environment env;

	@Autowired
	private CommandDispatcherListener commandDispatcherListener;

	//Keyword Finder
	@Autowired
	private KeywordListener keywordListener;

	public static void main(String[] args) {
		SpringApplication.run(RratBotApplication.class, args);
	}
	@Bean
	@ConfigurationProperties(value = "discord-api")
	//Initial configuration and authentication
	public DiscordApi discordApi(){
		String token = env.getProperty("TOKEN");
		//Set token and check login and join
		DiscordApi api = new DiscordApiBuilder().setToken(token).setAllNonPrivilegedIntents().login().join();

		//Listeners
		api.addMessageCreateListener(commandDispatcherListener);
		api.addMessageCreateListener(keywordListener);

		return api;
	}
}
