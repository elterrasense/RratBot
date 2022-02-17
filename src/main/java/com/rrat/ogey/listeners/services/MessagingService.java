package com.rrat.ogey.listeners.services;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;

import java.io.File;

public interface MessagingService {

    void sendMessage(String title, File attachment, TextChannel channel);
}
