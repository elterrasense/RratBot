package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

@Component
@BotCommand("nabemoment")
public class NabeLoreCommandExecutor implements CommandExecutor {

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments == null || "".equals(arguments)) {
            String[] lore = {
                    "Empty answer for random",
                    "Nabe orchestrated the Nanking massacre",
                    "Kson's battlestation (REAL) https://i.postimg.cc/26M7TxLT/unknown.png",
                    "Don't call women whores ever again.",
                    "https://i.postimg.cc/k51nJP3L/unknown.png",
                    "https://i.postimg.cc/7PgSLKhm/unknown.jpg",
                    "https://i.postimg.cc/mZCy4DNW/unknown.png",
                    "https://cdn.discordapp.com/attachments/270020168221065216/959690315952963644/usui_japanese_talk.mp4"
            };
            int response = (int) (Math.random() * (5) + 1);
            event.getChannel().sendMessage(lore[response]);
        } else {
            event.getChannel().sendMessage("This command doesn't use arguments retard.");
        }
    }
}
