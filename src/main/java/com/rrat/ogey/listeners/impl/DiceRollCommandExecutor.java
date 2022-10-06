package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

@Component
@BotCommand("roll")
public class DiceRollCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            try {
                int faces = Integer.parseInt(arguments);
                int response = (int) (Math.random() * (faces) + 1);
                event.getChannel().sendMessage(String.valueOf(response));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!roll [number]`?");
            }
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!roll [number]`?");
        }
    }
}
