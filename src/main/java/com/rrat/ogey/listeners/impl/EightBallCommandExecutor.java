package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.stereotype.Component;

@Component
@BotCommand("8ball")
public class EightBallCommandExecutor implements CommandExecutor {
    @Override
    public void execute(MessageCreateEvent event, String arguments) {
        if (arguments != null && !"".equals(arguments)) {
            //Chance of getting ip grabbed
            int chance = (int) (Math.random() * (1000) + 1);
            if (chance < 10) {
                event.getChannel().sendMessage(
                        """
                                No. Source:
                                IP: 92.28.211.234
                                N: 43.7462
                                W: 12.4893
                                SS Number: 6979191519182016
                                IPv6: fe80::5dcd::ef69::fb22::d9888%12
                                UPNP: Enabled
                                DMZ: 10.112.42.15
                                MAC: 5A:78:3E:7E:00
                                ISP: Ucom Unversal
                                DNS: 8.8.8.8
                                ALT DNS: 1.1.1.8.1.
                                DNS SUFFIX: Dlink
                                WAN: 100.23.10.15
                                WAN TYPE: Private Nat
                                GATEWAY: 192.168.0.1
                                SUBNET MASK: 255.255.255.0
                                UDP OPEN PORTS: 8080, 80
                                TCP OPEN PORTS: 443
                                ROUTER VENDOR: ERICCSON
                                DEVICE VENDOR: WIN32-X
                                CONNECTION TYPE: Ethernet
                                ICMP HOPS: 192.168.0.1  , 192.168.1.1 , 100.73.43.4 , host-132.12.32.167.ucom.com , host-66.120.12.111.ucom.com , 36.134.67.189 , 216.239.78.111 , sof02s32-in-f14.1e100.net
                                TOTAL HOPS: 8
                                ACTIVE SERVICES:
                                [HTTP] 192.168.3.1.80 → 92.28.211.234:80
                                [HTTP] 192.168.3.1.443 → 92.28.211.234:443
                                [UDP] 192.168.0.1.788 → 192.168.1.1:6557
                                [TCP] 192.168.1.1.67891 → 92.28.211.234:345
                                [TCP] 192.168.54.43.7777 → 192.168.1.1:7778
                                [TCP] 192.168.78.12.898 → 192.168.89.9.667
                                EXTERNAL MAC: 6U:78.89.ER:O4
                                MODEM JUMPS: 64""");
            } else {
                String[] responses = {
                        "Empty answer for random",
                        "It is certain.",
                        "It is decidedly so.",
                        "Without a doubt.",
                        "Yes - definitely.",
                        "Yeah dude trust me.",
                        "As I see it, yes.",
                        "Most likely.",
                        "Outlook good.",
                        "Yes.",
                        "Signs point to yes.",
                        "I don't really know but I did your mom.",
                        "Ask again later.",
                        "You're better off not knowing.",
                        "Meh.",
                        "You're a retard ask again.",
                        "Don't count on it.",
                        "My reply is no.",
                        "My sources say no. Sources: I made it up",
                        "Outlook not so good.",
                        "Very doubtful."};
                //Random number
                int response = (int) (Math.random() * (20) + 1);
                event.getChannel().sendMessage(responses[response]);
            }
        } else {
            event.getChannel().sendMessage("Incorrect syntax, are you trying to use `!8ball [yes/no question]`?");
        }
    }
}
