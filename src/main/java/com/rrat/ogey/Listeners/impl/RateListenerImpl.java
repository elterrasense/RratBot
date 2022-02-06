package com.rrat.ogey.Listeners.impl;

import com.rrat.ogey.Listeners.RateListener;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RateListenerImpl implements RateListener {
    //Pattern finder
    private final static Pattern pattern = Pattern.compile("!rateself (.+)");

    @Override
    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        if (messageCreateEvent.getMessageContent().startsWith("!rateself")) {
            Matcher matcher = pattern.matcher(messageCreateEvent.getMessageContent());
            if (matcher.matches()) {
                //Combine the hashes of username and input to achieve a random number between 1 and 100
                int rating = 1 + Math.abs(Objects.hash(messageCreateEvent.getMessageAuthor().getDisplayName() ,
                        matcher.group(1))) % 100;
                //Send message
                messageCreateEvent.getChannel()
                        .sendMessage(
                            messageCreateEvent.getMessageAuthor().getDisplayName() + " is " + rating + "% "
                                    + matcher.group(1));
            } else {
                //Send help syntax message
                messageCreateEvent.getChannel().sendMessage("Incorrect syntax, are you trying to use " +
                        "`!rateself [word]`?");
            }
        }
    }
}
