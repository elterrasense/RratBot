package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.listeners.services.MessagingService;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RateCommandExecutor implements CommandExecutor {

    @Autowired
    private MessagingService messagingService;

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            int rating = 1 + Math.abs(Objects.hash(event.getMessageAuthor().getDisplayName(), arguments)) % 100;
            messagingService.sendMessage(event.getMessageAuthor().getDisplayName() + " is " + rating + "% " + arguments,
                    null,
                    event.getChannel()
            );
        } else {
            messagingService.sendMessage("Incorrect syntax, are you trying to use `!rateself [word]`?",
                    null,
                    event.getChannel()
            );
        }
    }
}
