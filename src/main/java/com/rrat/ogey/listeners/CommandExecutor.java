package com.rrat.ogey.listeners;

import org.javacord.api.event.message.MessageCreateEvent;

/** @author Dmitry Kozlov <d.kozlov@haulmont.com> */
public interface CommandExecutor {
    void execute(MessageCreateEvent event, String arguments);
}
