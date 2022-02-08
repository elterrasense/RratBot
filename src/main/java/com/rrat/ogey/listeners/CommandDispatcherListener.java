package com.rrat.ogey.listeners;

import com.rrat.ogey.listeners.impl.RateCommandExecutor;
import com.rrat.ogey.listeners.impl.RateThingCommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CommandDispatcherListener implements MessageCreateListener {

    private static final Pattern CMD_PATTERN = Pattern.compile("^!(?<command>[^\\s]+)(?:\\s(?<arguments>.+))?$");
    private final HashMap<String, CommandExecutor> commands = new HashMap<>();

    @Autowired
    private RateCommandExecutor rateSelf;

    @Autowired
    private RateThingCommandExecutor rateThing;

    @PostConstruct
    private void postConstruct() {
        commands.put("rateself", rateSelf);
        commands.put("rate", rateThing);
        commands.put("ping", (event, args) -> {
            event.getChannel().sendMessage("Pong!");
        });
        commands.put("help", (event, args) -> {
            event.getChannel().sendMessage(
                    "You can find the currently available commands here: " +
                            "<https://github.com/elterrasense/RratBot/blob/main/README.md#currently-available-commands>");
        });
    }

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        String text = ev.getMessageContent();
        Matcher matcher = CMD_PATTERN.matcher(text);
        if (matcher.matches()) {
            CommandExecutor executor = commands.get(matcher.group("command"));
            if (executor != null) {
                executor.execute(ev, matcher.group("arguments"));
            }
        }
    }
}
