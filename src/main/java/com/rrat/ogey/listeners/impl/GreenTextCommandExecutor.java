package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.components.MarkovModelComponent;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GreenTextCommandExecutor implements CommandExecutor {

    @Autowired
    private MarkovModelComponent markov;

    @Override
    public void execute(MessageCreateEvent ev, String arguments) {
        markov.generateQuote().thenAccept(story -> {
            if (story.isPresent()) {
                ev.getChannel().sendMessage(story.get());
            } else {
                ev.getChannel().sendMessage("I have no stories to tell...");
            }
        });
    }
}
