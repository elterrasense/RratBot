package com.rrat.ogey.listeners.services;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;

@Component
public class MessagingServiceImpl implements MessagingService {
    @Override
    public void sendMessage(String title, File attachment, TextChannel channel) {
        //Random footer
        String[] phrases = {
                "Void",
                "You should play Apex... NOW",
                "How about you get some bitches on your dick",
                "Need an oil change",
                "Tow...",
                "Kuri please shut the fuck up",
                "Ban Rouge when",
                "Bon please come back",
                "Edel isn't real",
                "IP: 92.28.211.234",
                "Nabe moment"
        };
        int footer = (int) (Math.random() * (9) + 1);

        new MessageBuilder().setEmbed(new EmbedBuilder()
                        .setTitle(title)
                        .setImage(attachment)
                        .setFooter(phrases[footer])
                        .setColor(new Color(167, 11, 11)))
                .send(channel);
    }
}
