package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

@Component
@BotCommand("rate")
public class RateThingCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            AllowedMentionsBuilder allowedMentions = new AllowedMentionsBuilder();
            allowedMentions.setMentionEveryoneAndHere(false).setMentionRoles(false).setMentionUsers(false);
            int chance = (int) (Math.random() * (1000) + 1);
            if (chance < 10) {
                MessageBuilder message = new MessageBuilder();
                message.setAllowedMentions(allowedMentions.build()).append("I rate " + arguments + " 11/10").send(event.getChannel());
            } else {
                int rating = 1 + (Math.abs(arguments.hashCode()) % 10);
                MessageBuilder message = new MessageBuilder();
                message.setAllowedMentions(allowedMentions.build()).append("I rate " + arguments + " " + rating + "/10").send(event.getChannel());
            }
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!rate [thing]`?");
        }
    }
}
