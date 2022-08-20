package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.*;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.sticker.StickerFormatType;
import org.javacord.api.entity.sticker.StickerItem;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


@Component
@BotCommand("webhook")
public class ServerCrosspostCommandExecutor implements CommandExecutor,MessageCreateListener, ReactionAddListener {

    private static final Pattern pt_webhook = Pattern.compile("https://discord.com/api/webhooks/\\d{18}/.+");
    private String mainserver = null;
    private String webhookurl = null;
    private String mainchannel = null;
    private static String serverid = null;
    private static final ArrayList<String> ignoredchannels = new ArrayList<>();
    private final LinkedHashMap<String, String> messagemirror = new LinkedHashMap<>() {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 1001;
        }
    };
    private static ConcurrentHashMap<String, String> channelmirror = new ConcurrentHashMap<>();


    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        String[] args = arguments.split(" ");
        if (pt_webhook.matcher(arguments).matches()) {
            webhookurl = arguments;
        } else {
            switch (args[0]) {
                case "close" -> webhookurl = null;
                case "create" -> { //Gets an already made webhook by itself, otherwise creates one
                    List<Webhook> webhookList = event.getChannel().getAllIncomingWebhooks().join();
                    Optional<IncomingWebhook> createdwebhook = webhookList.stream()
                            .filter(Webhook::isIncomingWebhook)
                            .filter(webhook -> webhook.getCreator().map(User::isYourself).orElse(false))
                            .findFirst()
                            .flatMap(Webhook::asIncomingWebhook);
                    if (createdwebhook.isPresent()) {
                        webhookurl = createdwebhook.get().getUrl().toString();
                    } else {
                        WebhookBuilder webhookBuilder = new WebhookBuilder(event.getChannel().asServerTextChannel().orElse(null));
                        webhookBuilder.setAvatar(event.getApi().getYourself().getAvatar())
                                .setName("Copy-webhook")
                                .setAuditLogReason("So yeah");
                        IncomingWebhook incwebhook = webhookBuilder.create().join();
                        webhookurl = incwebhook.getUrl().toString();
                    }
                    mainchannel = event.getChannel().getIdAsString();
                    mainserver = event.getServer().map(Server::getIdAsString).orElse(null);
                }
                case "setid" -> serverid = args[1]; // Set the id of the server you want it to view
                case "link" -> { // Link a certain channel to a thread
                    if (args[1].equals("clear"))
                        channelmirror.remove(args[2]);
                    else
                        channelmirror.put(args[1], args[2]);
                }
                case "ignore" -> { //Ignore a certain channel
                    if (args[1].equals("clear"))
                        ignoredchannels.remove(args[2]);
                    else
                        ignoredchannels.add(args[1]);
                }
                case "save" -> save(); //Save settings for later use
            }
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        String channelid = ev.getChannel().getIdAsString();
        if (webhookurl != null && ev.getMessageAuthor().isRegularUser() | ev.getMessageAuthor().isYourself() && ev.getServer().map(Server::getIdAsString).orElse("1").equals(serverid) && !ignoredchannels.contains(channelid)) {
            String url = webhookurl;
            String channeloverride = mainchannel;
            String displayname = ev.getMessageAuthor().getDisplayName();
            Message message = ev.getMessage();
            if (channelmirror.containsKey(channelid)) {
                channeloverride = channelmirror.get(channelid);
                url = webhookurl.concat("?thread_id=").concat(channeloverride);
            } else {
                displayname = displayname + " in #" + ev.getChannel().toString().substring(ev.getChannel().toString().indexOf("name:") + 6, ev.getChannel().toString().length() - 1);
            }
            WebhookMessageBuilder webhookMessageBuilder = new WebhookMessageBuilder();
            AllowedMentionsBuilder allowedMentionsBuilder = new AllowedMentionsBuilder();
            allowedMentionsBuilder.setMentionEveryoneAndHere(false).setMentionUsers(false).setMentionRoles(false);
            webhookMessageBuilder
                    .append(parseMessage(ev.getMessage()))
                    .setDisplayName(displayname)
                    .setDisplayAvatar(ev.getMessageAuthor().getAvatar());
            webhookMessageBuilder.setAllowedMentions(allowedMentionsBuilder.build());

            Optional<Message> referencedmessage = message.getReferencedMessage();
            if (referencedmessage.isPresent() && messagemirror.containsKey(referencedmessage.get().getIdAsString())) {
                String messageid = messagemirror.get(referencedmessage.get().getIdAsString());
                String messagetext = referencedmessage.get().getReadableContent();
                if (messagetext == null)
                    messagetext = "Attachment/Sticker";
                if (messagetext.length() > 24)
                    messagetext = messagetext.substring(0, 24) + "...";
                webhookMessageBuilder.addComponents(
                        ActionRow.of(
                                Button.primary("nothing", referencedmessage.get().getAuthor().getDisplayName(), true),
                                Button.link("https://discord.com/channels/" + mainserver + "/" + channeloverride + "/" + messageid, messagetext)));
            }
            Message msg;
            msg = webhookMessageBuilder.send(ev.getApi(), url).join();
            messagemirror.put(message.getIdAsString(), msg.getIdAsString());
        } else if (webhookurl != null && channelmirror.contains(channelid)){
            if (ev.getMessageAuthor().isRegularUser() && !ev.getMessageContent().startsWith("!webhook"))
                ev.addReactionsToMessage("✅", "❌");
        } else if (webhookurl != null && mainchannel.equals(channelid))
            if (ev.getMessageAuthor().isRegularUser() && ev.getMessage().getReferencedMessage().isPresent())
                ev.addReactionsToMessage("✅", "❌");
    }

    @Override
    public void onReactionAdd(ReactionAddEvent ev) {
        Emoji reaction = ev.getEmoji();
        String server = ev.getServer().map(Server::getIdAsString).orElse("1");
        String userID = ev.getUserIdAsString();
        if (webhookurl != null && ev.getMessage().isPresent() && !ev.getUser().map(User::isYourself).orElse(false)) {
            Message msg = ev.getMessage().get();
            if (server.equals(serverid) && reaction.isKnownCustomEmoji() && messagemirror.containsKey(msg.getIdAsString()))
                ev.getApi().getCachedMessageById(messagemirror.get(msg.getIdAsString()))
                        .ifPresent(message -> message.addReaction(reaction));
            else if (ev.getReaction().map(Reaction::containsYou).orElse(false) && msg.getAuthor().getIdAsString().equals(userID)) {
                if (userID.equals(ev.getApi().getYourself().getIdAsString()))
                    return;
                if ("✅".equals(reaction.asUnicodeEmoji().orElse(null)))
                    sendmessage(msg, ev);
                else if ("❌".equals(reaction.asUnicodeEmoji().orElse(null)))
                    ev.removeReactionsByEmojiFromMessage("✅", "❌");
            }
            else if (!userID.equals(ev.getApi().getYourself().getIdAsString())) {
                ev.getApi().getCachedMessageById(getMessageMirror(msg.getIdAsString()))
                        .ifPresent(message -> message.addReaction(reaction));
            }
        }
    }

    private void sendmessage(Message msg, ReactionAddEvent ev){
        MessageBuilder messagetosend = new MessageBuilder();
        messagetosend.append(parseMessage(msg));
        msg.getStickerItems().stream()
                .filter(stickerItem -> stickerItem.getFormatType() == StickerFormatType.LOTTIE)
                .map(StickerItem::getId)
                .map(messagetosend::addSticker);
        Optional<Message> reference = msg.getReferencedMessage();
        if (reference.isPresent() && messagemirror.containsValue(reference.get().getIdAsString()))
            ev.getApi()
                    .getCachedMessageById(getMessageMirror(reference.get().getIdAsString()))
                    .ifPresent(message -> messagetosend.replyTo(message).send(message.getChannel()));
        else if (channelmirror.contains(ev.getChannel().getIdAsString()))
            ev.getApi()
                    .getTextChannelById(getChannelMirror(ev.getChannel().getIdAsString()))
                    .ifPresent(messagetosend::send);
    }

    private static void save() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".webhook-settings.obj");
            FileOutputStream fos = new FileOutputStream(path.toFile());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(serverid);
            oos.writeObject(ignoredchannels.toArray(String[]::new));
            oos.writeObject(channelmirror);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static void load(){
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".webhook-settings.obj");
            if (path.toFile().exists()) {
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis);
                serverid = (String) ois.readObject();
                String[] ignoredchannelarray = (String[]) ois.readObject();
                ignoredchannels.addAll(Arrays.asList(ignoredchannelarray));
                channelmirror = (ConcurrentHashMap<String, String>) ois.readObject();
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String parseMessage(Message msg) {
        String[] urls = msg.getAttachments().stream()
                .map(MessageAttachment::getUrl)
                .map(URL::toString)
                .toArray(String[]::new);
        String messagecontent = msg.getContent();
        if (messagecontent == null)
            messagecontent = "";
        for (String url : urls)
            messagecontent = messagecontent.concat("\n" + url);
        for (StickerItem sticker : msg.getStickerItems())
            if (sticker.getFormatType() == StickerFormatType.PNG || sticker.getFormatType() == StickerFormatType.APNG) {
                messagecontent = messagecontent.concat("\nhttps://media.discordapp.net/stickers/" + sticker.getIdAsString() + ".png");
            }
        if (messagecontent.equals(""))
            messagecontent = "*Sticker/Nothing*";
        return messagecontent;
    }

    private String getChannelMirror(String channelid) {
        for (Map.Entry<String, String> set : channelmirror.entrySet()) {
            if (set.getValue().equals(channelid))
                return set.getKey();
        }
        return null;
    }

    private String getMessageMirror(String messageid) {
        String[] keyset = messagemirror.keySet().toArray(String[]::new);
        int left = 0;
        int right = messagemirror.size() - 1;
        while (right >= left) {
            int middle = (left + right) / 2;
            if (Long.parseLong(messagemirror.get(keyset[middle])) == Long.parseLong(messageid)) {
                return keyset[middle];
            } else if (Long.parseLong(messagemirror.get(keyset[middle])) > Long.parseLong(messageid)) {
                right = middle - 1;
            } else if (Long.parseLong(messagemirror.get(keyset[middle])) < Long.parseLong(messageid)) {
                left = middle + 1;
            }
        }
        return null;
    }
}
