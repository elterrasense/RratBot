package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

@Component
public class FortuneCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        //Small chance of rare answer
        int chance = (int) (Math.random() * (1000) + 1);
        if (chance < 10) {
            String[] rareResponse = {
                    "Empty answer for random",
                    "Your fortune: le ebin dubs xDDDDDDDDDDDD",
                    "Your fortune: you gon' get some dick",
                    "Your fortune: ayy lmao",
                    "Your fortune: (YOU ARE BANNED)",
                    "Your fortune: Get Shrekt",
                    "Your fortune: YOU JUST LOST THE GAME",
                    "Your fortune: NOT SO SENPAI BAKA~KUN",
            };
            int response = (int) (Math.random() * (7) + 1);
            new MessageBuilder().replyTo(event.getMessageId()).setContent(
                    rareResponse[response]).send(event.getChannel()
            );
        } else {
            String[] responses = {
                    "Empty answer for random",
                    "Your fortune: Reply hazy, try again",
                    "Your fortune: Excellent Luck",
                    "Your fortune: Good Luck",
                    "Your fortune: Average Luck",
                    "Your fortune: Bad Luck",
                    "Your fortune: Good news will come to you by mail",
                    "Your fortune: （　´_ゝ`）ﾌｰﾝ ",
                    "Your fortune: ｷﾀ━━━━━━(ﾟ∀ﾟ)━━━━━━ !!!!",
                    "Your fortune: You will meet a dark handsome stranger",
                    "Your fortune: Better not tell you now",
                    "Your fortune: Outlook good",
                    "Your fortune: Very Bad Luck",
                    "Your fortune: Godly Luck"
            };
            int response = (int) (Math.random() * (13) + 1);
            new MessageBuilder().replyTo(event.getMessageId()).setContent(
                    responses[response]).send(event.getChannel()
            );
        }
    }
}

