package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RateCommandExecutor implements CommandExecutor {

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            MessageAuthor author = event.getMessageAuthor();
            String username = author.getDisplayName();
            int rating = 1 + Math.abs(Objects.hash(username, arguments)) % 100;
            event.getChannel().sendMessage(username + " is " + rating + "% " + arguments);
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!rateself [word]`?");
        }
    }
}
