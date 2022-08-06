package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.WebhookMessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@BotCommand("embedlinks")
@Component
public class EmbedCommandExecutor implements MessageCreateListener, CommandExecutor {

    public static final Pattern pt_discordlink = Pattern.compile("https://discord.com/channels/(?<serverID>\\d+)/(?<ChannelID>\\d+)/(?<MessageID>\\d+)");
    private boolean status = true;
    private String serverid = "1";

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        String[] args = arguments.split(" ");
        if (args.length >= 1){
            switch (args[0]){
                case "enable" -> status = true;
                case "disable" -> status = false;
                case "id" -> serverid = args[1];
            }
        }

    }

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        String messagecontent = ev.getMessageContent();

        if (status && serverid.equals(ev.getServer().map(Server::getIdAsString).orElse(null)) && ev.getMessageAuthor().isRegularUser()){
            Matcher matches = pt_discordlink.matcher(messagecontent);
            List<MatchResult> matchresults = matches.results().toList();
            if (matchresults.size() != 0) {
                messagecontent = messagecontent.replaceAll(pt_discordlink.pattern(), "");
                WebhookMessageBuilder webhook = new WebhookMessageBuilder();
                AllowedMentionsBuilder allowedMentions = new AllowedMentionsBuilder();
                allowedMentions.setMentionEveryoneAndHere(false).setMentionRoles(false).setMentionUsers(false);
                webhook.setAllowedMentions(allowedMentions.build())
                        .setDisplayName(ev.getMessageAuthor().getDisplayName())
                        .setDisplayAvatar(ev.getMessageAuthor().getAvatar())
                        .append(messagecontent);
                checkReply(ev, webhook);

                for (MatchResult mr : matchresults) {
                    Optional<CompletableFuture<Message>> embedmessage = ev.getApi().getMessageByLink(mr.group());
                    if (embedmessage.isPresent()) {
                        Message message = embedmessage.get().join();
                        EmbedBuilder messageembed = new EmbedBuilder();
                        messageembed.setAuthor(message.getAuthor().getDisplayName(), mr.group(), message.getAuthor().getAvatar())
                                .setColor(Color.WHITE)
                                .setTimestamp(message.getCreationTimestamp())
                                .setDescription(message.getContent());
                        message.getAttachments().stream().filter(MessageAttachment::isImage).findFirst()
                                .map(MessageAttachment::getUrl).map(URL::toString).ifPresent(messageembed::setImage);
                        webhook.addEmbed(messageembed);
                        if (message.getEmbeds().size() < 3)
                            for (Embed embed : message.getEmbeds())
                                if (embed.getType().equals("rich"))
                                    webhook.addEmbed(embed.toBuilder());
                    }
                }
                ev.deleteMessage();
                webhook.send(ev.getApi(), getWebhook(ev));
            }
        }
    }

    private void checkReply(MessageCreateEvent message,WebhookMessageBuilder webhook){
        Optional<Message> referencedmessage = message.getMessage().getReferencedMessage();
        if (referencedmessage.isPresent()) {
            Message refmsg = referencedmessage.get();
            String messagetext = refmsg.getReadableContent();
            if (messagetext == null)
                messagetext = "Attachment/Sticker";
            if (messagetext.length() > 24)
                messagetext = messagetext.substring(0, 24) + "...";
            webhook.addComponents(
                    ActionRow.of(
                            Button.primary("nothing", refmsg.getAuthor().getDisplayName(), true),
                            Button.link(refmsg.getLink().toString(), messagetext)));
        }
    }

    private String getWebhook(MessageCreateEvent event){
        List<Webhook> webhookList = event.getChannel().getAllIncomingWebhooks().join();
        Optional<IncomingWebhook> createdwebhook = webhookList.stream()
                .filter(Webhook::isIncomingWebhook)
                .filter(webhook -> webhook.getCreator().map(User::isYourself).orElse(false))
                .findFirst()
                .flatMap(Webhook::asIncomingWebhook);
        if (createdwebhook.isPresent())
            return createdwebhook.get().getUrl().toString();
        else {
            WebhookBuilder webhookBuilder = new WebhookBuilder(event.getChannel().asServerTextChannel().orElse(null));
            webhookBuilder.setAvatar(event.getApi().getYourself().getAvatar())
                    .setName("Embed-webhook")
                    .setAuditLogReason("Webhook to create embeds");
            IncomingWebhook incwebhook = webhookBuilder.create().join();
            return incwebhook.getUrl().toString();
        }
    }

}
