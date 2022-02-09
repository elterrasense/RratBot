package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;

@Component
public class CockRateCommandExecutor implements CommandExecutor {
    //Formatting the double like this as I don't know if .sendMessage supports %.2f, and I can't bother to find out now
    private static final DecimalFormat df = new DecimalFormat("0.000");

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            //Get a length from 1 cm to 25 cm then convert it to imperial
            double lengthCm = 1 + (Math.abs(arguments.hashCode()) % 25);
            double lengthInch =  lengthCm * 0.394;
            event.getChannel().sendMessage(arguments + "'s" + " cock is " + lengthCm + " cm/"
                    + df.format(lengthInch) + " inches long");
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!cockrate [thing]`?");
        }
    }
}
