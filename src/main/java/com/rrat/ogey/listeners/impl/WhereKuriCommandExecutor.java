package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

@Component
public class WhereKuriCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            String[] responses = {
                    "Empty answer for random",
                    "Spain",
                    "France",
                    "Brazil",
                    "Philippines",
                    "Japan",
                    "United States",
                    "Madagascar",
                    "Germany",
                    "Russia"};
            //Random number
            int response = (int) (Math.random() * (9) + 1);
            event.getChannel().sendMessage(responses[response]);
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!8ball " +
                    "[yes/no question]`?");
        }
    }
}

