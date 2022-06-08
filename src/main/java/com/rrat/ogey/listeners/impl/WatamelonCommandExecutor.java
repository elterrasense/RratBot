package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.Random;

@Component
@BotCommand("watamelon")
public class WatamelonCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments){
        if (arguments == null || "".equals(arguments)){
            File dir = new File("Watamelons");
            //Create array of file names
            String[] files = dir.list();
            //Get a random file
            int image = new Random().nextInt(files.length);
            File attachment = new File(dir + "/" + files[image]);
            new MessageBuilder().addAttachment(attachment).send(event.getChannel());
        } else {
            event.getChannel().sendMessage("Incorrect syntax, this command doesn't use arguments.");
        }
    }
}
