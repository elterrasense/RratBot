package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.listeners.services.MessagingService;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RateThingCommandExecutor implements CommandExecutor {

    @Autowired
    MessagingService messagingService;

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            int chance = (int) (Math.random() * (1000) + 1);
            if (chance < 10) {
                messagingService.sendMessage("I rate " + arguments + " 11/10",
                        null,
                        event.getChannel()
                );
            } else {
                //Rating from 0 to 10 using hashing
                int rating = 1 + (Math.abs(arguments.hashCode()) % 10);
                messagingService.sendMessage("I rate " + arguments + " " + rating + "/10",
                        null,
                        event.getChannel()
                );
            }
        } else {
            messagingService.sendMessage("Incorrect syntax, are you trying to use `!rate [thing]`?",
                    null,
                    event.getChannel()
            );
        }
    }
}
