package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.listeners.services.MessagingService;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Random;

@Component
public class CockRateCommandExecutor implements CommandExecutor {
    private static final DecimalFormat df = new DecimalFormat("0.000");

    @Autowired
    private MessagingService messagingService;

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            //Get a length from 1 cm to 25 cm then convert it to imperial
            double lengthCm = 10 + 10 * (new Random(arguments.hashCode()).nextDouble());
            double lengthInch = lengthCm * 0.394;
            messagingService.sendMessage(arguments + "'s" + " cock is " + df.format(lengthCm) + " cm/"
                            + df.format(lengthInch) + " inches long",
                    null,
                    event.getChannel()
            );
        } else {
            messagingService.sendMessage("Incorrect syntax, are you trying to use `!cockrate [thing]`?",
                    null,
                    event.getChannel()
            );
        }
    }
}
