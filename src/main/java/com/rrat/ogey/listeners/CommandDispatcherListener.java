package com.rrat.ogey.listeners;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CommandDispatcherListener implements MessageCreateListener {

    private static final Pattern CMD_PATTERN = Pattern.compile("^!(?<command>[^\\s]+)(?:\\s+(?<arguments>.+))?$");

    private final HashMap<String, CommandExecutor> registry = new HashMap<>();

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    private void postConstruct() {
        Map<String, Object> commands = context.getBeansWithAnnotation(BotCommand.class);
        for (Object component : commands.values()) {
            if (component instanceof CommandExecutor) {
                Class<?> type = component.getClass();
                String command = type.getAnnotation(BotCommand.class).value();
                registry.put(command, (CommandExecutor) component);
            }
        }
        registry.put("help", (event, args) -> event.getChannel().sendMessage(
                "You can find the currently available commands here: " +
                        "<https://github.com/elterrasense/RratBot/blob/main/README.md#currently-available-commands>"));
    }

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        String text = ev.getMessageContent();
        Matcher matcher = CMD_PATTERN.matcher(text);
        if (matcher.matches()) {
            CommandExecutor executor = registry.get(matcher.group("command"));
            if (executor != null) {
                executor.execute(ev, matcher.group("arguments"));
            }
        }
    }
}
