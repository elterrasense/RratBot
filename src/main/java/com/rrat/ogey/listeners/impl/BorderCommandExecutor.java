package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.WebhookMessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.sticker.StickerFormatType;
import org.javacord.api.entity.sticker.StickerItem;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@BotCommand("borderlink")
@Component
public class BorderCommandExecutor implements CommandExecutor,MessageCreateListener {

    private static final Pattern pt_webhook = Pattern.compile("https://discord.com/api/webhooks/\\d{18}/.+");
    private String mainserver = null;
    private String webhookurl = null;
    private String mainchannel = null;
    private String borderchannel = null;
    private final LinkedHashMap<String, String> messagemirror = new LinkedHashMap<>() {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 201;
        }
    };


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
                    mainserver = event.getServer().get().getIdAsString();
                }
                case "border" -> borderchannel = args[1];
                case "relay" -> mainchannel = args[1];

            }
        }
    }


    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        if (webhookurl != null && ev.getMessageAuthor().isRegularUser() && ev.getChannel().getIdAsString().equals(borderchannel) ) {
            String url = webhookurl;
            String channeloverride = mainchannel;
            String displayname = ev.getMessageAuthor().getDisplayName();
            Message message = ev.getMessage();
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
        }
        else if (webhookurl != null && ev.getChannel().getIdAsString().equals(mainchannel) && ev.getMessageAuthor().isRegularUser()){
            sendmessage(ev);
        }
    }

    private void sendmessage(MessageCreateEvent ev){
        Message msg = ev.getMessage();
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
        else
            ev.getApi()
                    .getTextChannelById(borderchannel)
                    .ifPresent(messagetosend::send);
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

    private String parseMessage(Message msg) {
        String[] urls = msg.getAttachments().stream()
                .map(MessageAttachment::getUrl)
                .map(URL::toString)
                .toArray(String[]::new);
        String messagecontent = msg.getContent();
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
}
