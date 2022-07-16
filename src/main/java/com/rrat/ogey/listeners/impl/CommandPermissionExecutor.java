package com.rrat.ogey.listeners.impl;


import com.rrat.ogey.components.CommandPermissions;
import com.rrat.ogey.components.ServerIDnCommand;
import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@BotCommand("commands")
public class CommandPermissionExecutor implements CommandExecutor {

    private static final Pattern pt_all = Pattern.compile("<?(?<prefix>[#@]?&?)(?<id>\\d{18})>?");
    private static final ArrayList<String> deniedservers = new ArrayList<>(2);
    private static final ConcurrentHashMap<ServerIDnCommand, CommandPermissions> permissionsHashMap = new ConcurrentHashMap<>();

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        String[] args = arguments.split(" ");
        String serverid = event.getServer().map(DiscordEntity::getIdAsString).orElse(null);
        if (serverid != null && event.getMessageAuthor().canManageServer() && args.length > 1) {
            switch (args[0]) {
                case "enable" -> { //Allow a command for a certain role/user if it has been disabled
                    ServerIDnCommand cmdkey = new ServerIDnCommand(serverid, args[1]);
                    permissionsHashMap.putIfAbsent(cmdkey, new CommandPermissions());
                    if (args.length < 3)
                        permissionsHashMap.get(cmdkey).denyall = false;
                    else
                        permissionsHashMap.get(cmdkey).addAllowed(args[2]);
                }
                case "disable" -> { // Disable a command for a user/channel/role or altogether
                    ServerIDnCommand cmdkey = new ServerIDnCommand(serverid, args[1]);
                    permissionsHashMap.putIfAbsent(cmdkey, new CommandPermissions());
                    if (args.length < 3)
                        permissionsHashMap.get(cmdkey).denyall = true;
                    else
                        permissionsHashMap.get(cmdkey).addDenied(args[2]);
                }
                case "remove" -> { // Removes either disabled or enabled permission for that command.
                    if (args.length > 3) {
                        ServerIDnCommand cmdkey = new ServerIDnCommand(serverid, args[1]);
                        if (permissionsHashMap.containsKey(cmdkey))
                            permissionsHashMap.get(cmdkey).removePermission(args[2]);
                    }
                }
                case "clear" -> permissionsHashMap.remove(new ServerIDnCommand(serverid, args[1]));
                case "denyserver" -> deniedservers.add(args[1]); //Removes all commands and keywords from a server
                case "allowserver" -> deniedservers.removeIf(s -> s.equals(args[1]));
            }
            save();
        }
    }

    public static boolean check(ServerIDnCommand commandkey, MessageCreateEvent ev){
        CommandPermissions cmdperms = permissionsHashMap.get(commandkey);

        boolean canrun = true;
        if (deniedservers.stream().anyMatch(s -> s.equals(commandkey.serverid())) && !commandkey.commandprefix().equals("commands"))
            return false;
        else if (cmdperms == null)
            return true;
        else if (cmdperms.denyall)
            canrun = false;
        String[] allowedperms = cmdperms.getAllowed();
        String[] deniedperms = cmdperms.getDenied();

        for (String perms : deniedperms){
            Matcher matcher = pt_all.matcher(perms);
            if (matcher.matches())
                switch (matcher.group("prefix")) {
                case "@" -> {
                    if (ev.getMessageAuthor().getIdAsString().equals(perms.replaceAll("\\D+", "")))
                        canrun = false;
                }
                case "@&" -> {
                    if (ev.getMessageAuthor().asUser().get().getRoles(ev.getServer().get()).stream()
                            .map(Role::getIdAsString)
                            .anyMatch(s -> Objects.equals(s, perms.replaceAll("\\D+", ""))))
                        canrun = false;
                }
                case "#" -> {
                    if (ev.getChannel().getIdAsString().equals(perms.replaceAll("\\D+", "")))
                        canrun = false;
                }
            }
        }
        for (String perms : allowedperms){
            Matcher matcher = pt_all.matcher(perms);
            if (matcher.matches())
                switch (matcher.group("prefix")) {
                case "@" -> {
                    if (ev.getMessageAuthor().getIdAsString().equals(perms.replaceAll("\\D+", "")))
                        canrun = true;
                }
                case "@&" -> {
                    if (ev.getMessageAuthor().asUser().get().getRoles(ev.getServer().get()).stream()
                            .map(Role::getIdAsString)
                            .anyMatch(s -> Objects.equals(s, perms.replaceAll("\\D+", ""))))
                        canrun = true;
                }
                case "#" -> {
                    if (ev.getChannel().getIdAsString().equals(perms.replaceAll("\\D+", "")))
                        canrun = true;
                }
            }
        }
        return canrun;
    }


    private static void save() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".denied-servers.obj");
            FileOutputStream fos = new FileOutputStream(path.toFile());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(deniedservers.toArray(String[]::new));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void load(){
        try {
            Path path = Paths.get(System.getProperty("user.home"), ".denied-servers.obj");
            if (path.toFile().exists()) {
                FileInputStream fis = new FileInputStream(path.toFile());
                ObjectInputStream ois = new ObjectInputStream(fis);
                String[] deniedserversarray = (String[]) ois.readObject();
                deniedservers.addAll(Arrays.asList(deniedserversarray));
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkserver(String serverid) {
        if (serverid == null)
            return true;
        else
            return !deniedservers.contains(serverid);
    }
}
