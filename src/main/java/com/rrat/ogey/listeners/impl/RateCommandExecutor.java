package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@BotCommand("rateself")
public class RateCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            int rating = 1 + Math.abs(Objects.hash(event.getMessageAuthor().getDisplayName(), arguments)) % 100;
            event.getChannel().sendMessage(event.getMessageAuthor().getDisplayName() + " is " + rating + "% " + arguments);
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!rateself [word]`?");
        }
    }
}
