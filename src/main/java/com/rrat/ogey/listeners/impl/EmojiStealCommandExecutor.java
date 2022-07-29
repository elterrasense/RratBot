package com.rrat.ogey.listeners.impl;


import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.emoji.CustomEmojiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.sticker.StickerItem;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@BotCommand("steal")
public class EmojiStealCommandExecutor implements CommandExecutor {

    private static final Pattern pt_emoji = Pattern.compile("<(?<animated>a)?:(?<name>\\w+):(?<id>\\d+)>");

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        Optional<Message> reference = event.getMessage().getReferencedMessage();
        Matcher matcher;
        if (arguments != null) {
            matcher = pt_emoji.matcher(arguments);
        } else
            matcher = reference
                    .map(message -> pt_emoji.matcher(message.getContent()))
                    .orElseGet(() -> pt_emoji.matcher(event.getMessage().getMessagesBefore(2).join().getNewestMessage().map(Message::getContent).orElse("1")));
        if (matcher.matches()) {
            String emojiname = matcher.group("name");
            String emojilink = "https://cdn.discordapp.com/emojis/" + matcher.group("id") + (matcher.group("animated") != null ? ".gif" : ".png");
            if (event.getMessageAuthor().canManageEmojisOnServer()) {
                CustomEmojiBuilder emojiBuilder = event.getServer().get().createCustomEmojiBuilder();
                try {
                    emojiBuilder.setName(emojiname)
                        .setImage(new URL(emojilink))
                        .create();
                } catch (MalformedURLException e) {e.printStackTrace();}
            }
            else{
                event.getChannel()
                        .sendMessage(emojilink);
            }
        }
        Optional<String> sticker = reference
                .map(Message::getStickerItems)
                .map(Set::iterator)
                .map(Iterator::next)
                .map(StickerItem::getIdAsString);
        sticker.ifPresent(string -> event.getChannel().sendMessage("https://media.discordapp.net/stickers/" + string + ".png"));
    }
}
