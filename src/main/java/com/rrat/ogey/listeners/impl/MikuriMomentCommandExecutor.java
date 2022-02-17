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
            File attachment = new File(dir + "/" + files[image]);
            messagingService.sendMessage("Requested by " + event.getMessageAuthor().getDisplayName(),
                    attachment,
                    event.getChannel()
            );
        } else {
            messagingService.sendMessage("Incorrect syntax, are you trying to use `!kurimoment`?",
                    null,
                    event.getChannel()
            );
        }
    }
}
