package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

@Component
@BotCommand("rate")
public class RateThingCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            int chance = (int) (Math.random() * (1000) + 1);
            if (chance < 10) {
                event.getChannel().sendMessage("I rate " + arguments + " 11/10");
            } else {
                int rating = 1 + (Math.abs(arguments.hashCode()) % 10);
                event.getChannel().sendMessage("I rate " + arguments + " " + rating + "/10");
            }
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!rate [thing]`?");
        }
    }
}
