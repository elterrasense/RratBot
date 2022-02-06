package com.rrat.ogey.Listeners.impl;

import com.rrat.ogey.Listeners.RateListener;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RateListenerImpl implements RateListener {
    //Pattern finder
    private final static Pattern pattern = Pattern.compile("!rate (\\w+)");

    @Override
    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        if (messageCreateEvent.getMessageContent().startsWith("!rate")) {
            Matcher matcher = pattern.matcher(messageCreateEvent.getMessageContent());
            if (matcher.matches()) {
                //Rating from 0 to 100 using Math.random
                int rating = (int) Math.floor(Math.random() * 100) + 1;
                messageCreateEvent.getChannel()
                        .sendMessage(
                            messageCreateEvent.getMessageAuthor().getDisplayName() + " is " + rating + "/100 " + matcher.group(1));
            } else {
                //Send help syntax message
                messageCreateEvent.getChannel().sendMessage("Incorrect syntax, are you trying to use `!rate [word]`?");
            }
        }
    }
}
