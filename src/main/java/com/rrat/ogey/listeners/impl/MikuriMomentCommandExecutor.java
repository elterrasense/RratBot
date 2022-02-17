package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.listeners.services.MessagingService;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Random;

@Component
public class MikuriMomentCommandExecutor implements CommandExecutor {

    @Autowired
    private MessagingService messagingService;
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments == null || "".equals(arguments)) {
            //Get a random image
            //Saved for testing purposes
            //File dir = Paths.get(System.getProperty("user.home"), "MikuriScreenshots").toFile();
            File dir = new File("MikuriScreenshots");
            String[] files = dir.list();
            int image = new Random().nextInt(files.length);
            File attachment = new File (dir + "/" + files[image]);
            messagingService.sendMessage("Requested by " + event.getMessageAuthor().getDisplayName(),
                    null,
                    attachment,
                    event.getChannel()
                    );
            /*
            //Random footer
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
            new MessageBuilder().setEmbed(new EmbedBuilder()
                    .setAuthor("Requested by " + event.getMessageAuthor().getDisplayName())
                    .setImage(new File (dir + "/" + files[image]))
                    .setFooter(phrases[footer])
                    .setColor(new Color(167, 11, 11)))
                    .send(event.getChannel());
             */
        } else {
            messagingService.sendMessage("Requested by " + event.getMessageAuthor(),
                    "Incorrect syntax, are you trying to use `!kurimoment`?",
                    null,
                    event.getChannel()
            );
        }
    }
}
