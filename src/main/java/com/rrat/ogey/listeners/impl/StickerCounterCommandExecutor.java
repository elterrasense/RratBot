package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.sticker.Sticker;
import org.javacord.api.entity.sticker.StickerItem;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@BotCommand("stickers")
@Component
public class StickerCounterCommandExecutor implements CommandExecutor, MessageCreateListener {

    private static String serverid = "1";
    private static ConcurrentHashMap<String,stickerinfo> idtosticker = new ConcurrentHashMap<>();
    int savecheck = 0;

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        String[] args = arguments.split(" ");
        switch (args[0]) {
            case "id" -> serverid = event.getServer().map(DiscordEntity::getIdAsString).orElse("1");
            case "leaderboard" -> {
                List<stickerinfo> stickerlist = idtosticker.values().stream().sorted(new StickerSort()).toList();
                EmbedBuilder embed = new EmbedBuilder();
                StringBuilder names = new StringBuilder();
                StringBuilder timesused = new StringBuilder();
                int max = 0;
                for (stickerinfo stickerinfo : stickerlist){
                    if (max > 60)
                        break;
                    names.append("[").append(stickerinfo.name).append("](https://media.discordapp.net/stickers/").append(stickerinfo.id).append(".png)").append("\n");
                    timesused.append(stickerinfo.usage).append("\n");
                    max++;
                }
                embed.addInlineField("Name", names.toString()).addInlineField("Times Used", timesused.toString());
                MessageBuilder message = new MessageBuilder();
                message.addEmbed(embed).replyTo(event.getMessage()).send(event.getChannel());
            }
            case "clear" -> {
                if (event.getMessageAuthor().canManageRolesOnServer())
                    idtosticker.clear();
            }
            case "list" -> {
                Set<Sticker> unusedstickers = new HashSet<>();
                Set<Sticker> stickers = event.getServer().get().getStickers();
                for (Sticker sticker : stickers){
                    if (!idtosticker.containsKey(sticker.getIdAsString())){
                        unusedstickers.add(sticker); //Add stickers that haven't been used at all in a separate category
                    }
                }
                List<String> idlist = stickers.stream().map(Sticker::getIdAsString).toList();
                List<stickerinfo> stickerlist = idtosticker.values().stream().filter(stickerinfo -> idlist.contains(stickerinfo.id)).sorted(new StickerSort().reversed()).toList();
                EmbedBuilder embed = new EmbedBuilder();
                StringBuilder names = new StringBuilder();
                StringBuilder timesused = new StringBuilder();
                MessageBuilder message = new MessageBuilder();
                int max = 0;
                for (stickerinfo stickerinfo : stickerlist){
                    if (max > 12)
                        break;
                    names.append("[").append(stickerinfo.name).append("](https://media.discordapp.net/stickers/").append(stickerinfo.id).append(".png)").append("\n");
                    timesused.append(stickerinfo.usage).append("\n");
                    max++;
                }
                if (!unusedstickers.isEmpty()){
                    EmbedBuilder embed2 = new EmbedBuilder();
                    StringBuilder unusednames = new StringBuilder();
                    for (Sticker sticker : unusedstickers){
                        unusednames.append("[").append(sticker.getName()).append("](https://media.discordapp.net/stickers/").append(sticker.getIdAsString()).append(".png)").append("\n");
                    }
                    embed2.setTitle("Unused Stickers Since Last Clear").setDescription(unusednames.toString());
                    message.addEmbed(embed2);
                }
                embed.setTitle("Least Used Stickers").addInlineField("Name", names.toString()).addInlineField("Times Used", timesused.toString());
                message.addEmbed(embed).replyTo(event.getMessage()).send(event.getChannel());
            }
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (serverid.equals(event.getServer().map(DiscordEntity::getIdAsString).orElse("404"))) {
            for (StickerItem sticker : event.getMessage().getStickerItems()) {
                idtosticker.putIfAbsent(sticker.getIdAsString(), new stickerinfo(sticker.getIdAsString(), sticker.getName()));
                stickerinfo stick = idtosticker.get(sticker.getIdAsString());
                stick.usage++;
            }
            savecheck++;
            if (savecheck > 20){
                save();
                savecheck = 0;
            }
        }
    }

    private static class stickerinfo implements Serializable{
        public final String id;
        public final String name;
        public int usage;
        private stickerinfo(String id, String name){
            this.id = id;
            this.name = name;
            this.usage = 0;
        }
    }
    private static class StickerSort implements Comparator<stickerinfo>{
        @Override
        public int compare(stickerinfo o1, stickerinfo o2) {
            return o2.usage - o1.usage;
        }
    }


    private void save() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".stickerinfo.obj");
            FileOutputStream fos = new FileOutputStream(path.toFile());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(serverid);
            oos.writeObject(idtosticker);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static void load(){
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".stickerinfo.obj");
            if (path.toFile().exists()) {
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis);
                serverid = (String) ois.readObject();
                idtosticker = (ConcurrentHashMap<String, stickerinfo>) ois.readObject();
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
