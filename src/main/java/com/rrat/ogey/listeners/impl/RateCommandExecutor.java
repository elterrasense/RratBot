package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@BotCommand("rateself")
public class RateCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            AllowedMentionsBuilder allowedMentions = new AllowedMentionsBuilder();
            allowedMentions.setMentionEveryoneAndHere(false).setMentionRoles(false).setMentionUsers(false);
            MessageBuilder message = new MessageBuilder();
            int rating = 1 + Math.abs(Objects.hash(event.getMessageAuthor().getDisplayName(), arguments)) % 100;
            message.setAllowedMentions(allowedMentions.build()).append(event.getMessageAuthor().getDisplayName() + " is " + rating + "% " + arguments).send(event.getChannel());
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!rateself [word]`?");
        }
    }
}
