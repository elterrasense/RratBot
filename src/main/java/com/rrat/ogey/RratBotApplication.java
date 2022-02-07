package com.rrat.ogey;

import com.rrat.ogey.Listeners.HelpListener;
import com.rrat.ogey.Listeners.PingListener;
import com.rrat.ogey.Listeners.RateListener;
import com.rrat.ogey.Listeners.RateThingListener;
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

	//Help
	@Autowired
	private HelpListener helpListener;

	//Ping
	@Autowired
	private PingListener pingListener;

	//Rate [word]
	@Autowired
	private RateListener rateListener;

	//Rate [thing]
	@Autowired
	private RateThingListener rateThingListener;

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
		api.addMessageCreateListener(helpListener);
		api.addMessageCreateListener(pingListener);
		api.addMessageCreateListener(rateListener);
		api.addMessageCreateListener(rateThingListener);

		return api;
	}
}
