package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.components.MarkovModelComponent;
import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.listeners.services.MessagingService;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@BotCommand("story")
public class GreenTextCommandExecutor implements CommandExecutor {

    @Autowired
    private MarkovModelComponent markov;

    @Autowired
    private MessagingService messagingService;

    @Override
    public void execute(MessageCreateEvent ev, String arguments) {
        markov.generateQuote().thenAccept(story -> {
            if (story.isPresent()) {
                messagingService.sendMessage(story.get(),
                        null,
                        ev.getChannel()
                );
            } else {
                messagingService.sendMessage("I have no stories to tell...",
                        null,
                        ev.getChannel()
                );
            }
        });
    }
}
