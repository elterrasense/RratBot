package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class MikuriMomentCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments == null) {
            //Get a random image from the screenshots folder

            String[] phrases = {
                    "Void",
                    "Kuri moment",
                    "Need an oil change",
                    "Tow...",
                    "Kuri please shut the fuck up",
                    "Ban Rouge when",
                    "Bon please come back",
                    "Edel isn't real",
                    "IP: 92.28.211.234",
                    "mfw pic related"
            };
            int footer = (int) (Math.random() * (9) + 1);
            EmbedBuilder embed = new EmbedBuilder()
                    .setAuthor("Requested by " + event.getMessageAuthor().getDisplayName())
                    .setImage()
                    .setFooter(phrases[footer]);
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!kurimoment`?");
        }
    }
}
