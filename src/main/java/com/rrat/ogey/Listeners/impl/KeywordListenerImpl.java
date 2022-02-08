package com.rrat.ogey.Listeners.impl;

import com.rrat.ogey.Listeners.KeywordListener;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KeywordListenerImpl implements KeywordListener {
    //Pattern finders
    private final static Pattern once = Pattern.compile("(?i)(^|\s)(once|11)(\s|$)");

    @Override
    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        Matcher matcherOnce = once.matcher(messageCreateEvent.getMessageContent());
        if (matcherOnce.find()) {
            messageCreateEvent.getChannel()
                    .sendMessage(
                            "Chúpala entonces");
        }
    }
}
