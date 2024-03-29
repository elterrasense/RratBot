package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.KeywordListener;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KeywordListenerImpl implements KeywordListener {
    //Pattern finders
    private final static Pattern once = Pattern.compile("(?i)(^|\s)(once|11)(\s|$)");
    private final static Pattern ayame = Pattern.compile("(?i)(^|\s)(ayame is a whore|ayame whore|whore)(\s|$)");
    private final static Pattern nabe = Pattern.compile("(?i)(^|\s)(nabe)(\s|$)");
    private final static Pattern ogeyrrat = Pattern.compile("(?i)(^|\s)(ogey|rrat)(\s|$)");
    private final static Pattern man = Pattern.compile("(?i)(^)(man)($)");
    private final static Pattern rushia = Pattern.compile("(?i)(^)(rushia)($)");

    @Override
    public void onMessageCreate(MessageCreateEvent messageCreateEvent) {
        //Condition to avoid bot matching other bot messages
        if (!messageCreateEvent.getMessageAuthor().isBotUser()) {
            //Condition to prohibit the bot from replying to messages from blacklisted servers.
            if (CommandPermissionExecutor.checkserver(messageCreateEvent.getServer().map(Server::getIdAsString).orElse(null))) {

                //Once
                Matcher matcherOnce = once.matcher(messageCreateEvent.getMessageContent());
                if (matcherOnce.find() && Math.random() < 0.2 ) {
                    messageCreateEvent.getChannel()
                            .sendMessage(
                                    "Chúpala entonces");
                }

                //Ayame is a whore
                Matcher matcherAyame = ayame.matcher(messageCreateEvent.getMessageContent());
                if (matcherAyame.find()) {
                    messageCreateEvent.getChannel()
                            .sendMessage(
                                    "<:ayamephone:823581452745703445>");
                }

                //Nabe
                Matcher matcherNabe = nabe.matcher(messageCreateEvent.getMessageContent());
                if (matcherNabe.find()) {
                    messageCreateEvent.getChannel()
                            .sendMessage(
                                    "<:towasip:906605142214860840>");
                }

                //Ogey rrat
                Matcher matcherOgey = ogeyrrat.matcher(messageCreateEvent.getMessageContent());
                if (matcherOgey.find()) {
                    if (messageCreateEvent.getMessageContent().toLowerCase().contains("ogey")) {
                        messageCreateEvent.getChannel()
                                .sendMessage(
                                        "rrat");
                    } else {
                        messageCreateEvent.getChannel()
                                .sendMessage(
                                        "ogey");
                    }
                }

                //man
                Matcher matcherMan = man.matcher(messageCreateEvent.getMessageContent());
                if (matcherMan.find()) {
                    messageCreateEvent.getChannel().sendMessage(":horse:");
                }

                //rushia
                Matcher matcherRushia = rushia.matcher(messageCreateEvent.getMessageContent());
                if (matcherRushia.find()) {
                    messageCreateEvent.getChannel()
                            .sendMessage(
                                    """
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃＼○／
                                            ┛┗┛┗┛┃ / /
                                            ┓┏┓┏┓┃ノ)
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            ┓┏┓┏┓┃
                                            ┛┗┛┗┛┃
                                            """);
                }
            }
        }
    }
}
