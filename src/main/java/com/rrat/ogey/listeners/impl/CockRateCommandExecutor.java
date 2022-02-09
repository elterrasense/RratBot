package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.Random;

@Component
public class CockRateCommandExecutor implements CommandExecutor {
    private static final DecimalFormat df = new DecimalFormat("0.000");

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            //Get a length from 1 cm to 25 cm then convert it to imperial
            double lengthCm = 10 + 10 * (new Random(arguments.hashCode()).nextDouble());
            double lengthInch =  lengthCm * 0.394;
            if (lengthCm > 0) {
                event.getChannel().sendMessage(arguments + "'s" + " cock is " + df.format(lengthCm) + " cm/"
                        + df.format(lengthInch) + " inches long");
            } else {
                //Variation if values are negative
                event.getChannel().sendMessage(arguments + "'s" + " cock is so short it became a vagina! It's"
                        + df.format(lengthCm) + " cm/" + df.format(lengthInch) + " inches deep");
            }
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!cockrate [thing]`?");
        }
    }
}
