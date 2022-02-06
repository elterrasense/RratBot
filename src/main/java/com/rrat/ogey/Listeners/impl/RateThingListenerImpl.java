package com.rrat.ogey.Listeners.impl;
import com.rrat.ogey.Listeners.RateThingListener;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RateThingListenerImpl implements RateThingListener {
    //Pattern finder
    private final static Pattern pattern = Pattern.compile("!rate (.+)");

    @Override
    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        if (messageCreateEvent.getMessageContent().startsWith("!rate")) {
            Matcher matcher = pattern.matcher(messageCreateEvent.getMessageContent());
            if (matcher.matches()) {
                //Small chance of giving 11/10
                int chance = (int) (Math.random()*(1000) + 1);
                if (chance < 10) {
                    messageCreateEvent.getChannel().sendMessage("I rate " + matcher.group(1) + " 11/10");
                } else {
                    //Rating from 0 to 10 using Math.random
                    int rating = (int) (Math.random()*(10))+ 1;
                    messageCreateEvent.getChannel()
                            .sendMessage(
                                    "I rate " + matcher.group(1) + " " + rating + "/10");
                }
            } else {
                //Send help syntax message
                messageCreateEvent.getChannel().sendMessage("Incorrect syntax, are you trying to use `!rate [thing]`?");
            }
        }
    }
}
