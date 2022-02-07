package com.rrat.ogey.Listeners.impl;

import com.rrat.ogey.Listeners.HelpListener;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HelpListenerImpl implements HelpListener {
    //Pattern finder
    private final static Pattern pattern = Pattern.compile("!help");
    @Override

    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        if (messageCreateEvent.getMessageContent().startsWith("!help")) {
            Matcher matcher = pattern.matcher(messageCreateEvent.getMessageContent());
            if (matcher.matches()) {
                //Send message
                messageCreateEvent.getChannel()
                        .sendMessage(
                                "You can find the currently available commands here: " +
                                        "<https://github.com/elterrasense/RratBot/blob/main/README.md#currently-available-commands>");
            } else {
                //Send help syntax message
                messageCreateEvent.getChannel().sendMessage("How did you fuck up the `!help` command");
            }
        }
    }
}
