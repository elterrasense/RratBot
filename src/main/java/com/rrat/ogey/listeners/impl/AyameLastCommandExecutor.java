package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@BotCommand("ayame")
public class AyameLastCommandExecutor implements CommandExecutor {

    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments == null || "".equals(arguments)) {
            try {
                //Request latest stream
                URL videoQuery = new URL("https://holodex.net/api/v2/channels/UC7fk0CB07ly8oSl0aqKkqFg/videos?limit=1");
                String videoData = new String(videoQuery.openStream().readAllBytes(), StandardCharsets.UTF_8);
                //Retrieve date of last stream
                Pattern dateFind = Pattern.compile("\"published_at\":\"((?<date>.+?)T)");
                Matcher matcher = dateFind.matcher(videoData);
                String date = null;
                if (matcher.find()) {
                    date = matcher.group("date");
                }
                //Calculate days since last stream
                LocalDate streamDate = LocalDate.parse(date);
                LocalDate currentDate = LocalDate.now();
                long days_difference = ChronoUnit.DAYS.between(streamDate, currentDate);
                //Reply
                event.getChannel().sendMessage(">Last stream: " + days_difference + " days ago.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            event.getChannel().sendMessage("Incorrect syntax, this command doesn't use arguments.");
        }
    }
}
