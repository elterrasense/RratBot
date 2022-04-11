package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
public class CoupleMatchExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            String trimArgs = arguments.trim();
            trimArgs = trimArgs.toLowerCase();
            String[] args = trimArgs.split("\s+");
            if (args.length == 2) {
                int rating = 1 + Math.abs(Objects.hash(args[0], args[1])) % 100;
                event.getChannel().sendMessage(
                        args[0] + " and " + args[1] + " are " + rating + "% compatible!"
                );
            } else {
                event.getChannel().sendMessage(
                        "Incorrect syntax, are you trying to use `!ship [argument] [argument 2]`?"
                );
            }
        } else {
            event.getChannel().sendMessage(
                    "Incorrect syntax, are you trying to use `!ship [argument] [argument 2]`?"
            );
        }
    }
}
