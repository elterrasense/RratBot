package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.listeners.services.MessagingService;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WhereKuriCommandExecutor implements CommandExecutor {

    @Autowired
    MessagingService messagingService;

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments == null || "".equals(arguments)) {
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
                    "Russia",
                    "Somalia",
                    "Iraq",
                    "Tienanmen",
                    "Kosovo",
                    "Israel",
                    "Turkey",
                    "Poland",
                    "Cambodia",
                    "Pyongyang",
                    "Venezuela",
                    "Serbia",
                    "Romania"
            };
            //Random number
            int response = (int) (Math.random() * (21) + 1);
            messagingService.sendMessage(responses[response],
                    null,
                    event.getChannel()
            );
        } else {
            messagingService.sendMessage("Incorrect syntax, are you trying to use ` !wherekuri`?",
                    null,
                    event.getChannel()
            );
        }
    }
}

