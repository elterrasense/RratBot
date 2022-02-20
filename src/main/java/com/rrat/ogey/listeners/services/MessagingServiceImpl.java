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
        new MessageBuilder().setEmbed(new EmbedBuilder()
                        .setTitle(title)
                        .setImage(attachment)
                        .setColor(new Color(167, 11, 11)))
                .send(channel);
    }
}
