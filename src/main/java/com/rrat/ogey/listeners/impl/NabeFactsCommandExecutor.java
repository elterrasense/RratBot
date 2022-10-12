package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.components.MarkovModelComponent;
import com.rrat.ogey.listeners.BotCommand;
import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.model.AnnotateTokenizer;
import com.rrat.ogey.model.AnnotatedTokens;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@BotCommand("nabefact")
public class NabeFactsCommandExecutor implements CommandExecutor {

    @Autowired
    private MarkovModelComponent markov;

    @Override
    public void execute(MessageCreateEvent ev, String arguments) {
        if (arguments != null) {
            AnnotateTokenizer tokenizer = AnnotateTokenizer.instance();
            spitFacts(ev, AnnotatedTokens.listWords(tokenizer.tokenize(arguments)), arguments);
        } else {
            spitFacts(ev, Collections.emptyList(), null);
        }
    }

    private void spitFacts(MessageCreateEvent ev, List<String> words, String query) {
        switch (words.size()) {
            case 0 ->
                markov.generateNabeAsync().thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        ev.getChannel().sendMessage(maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know no facts yet");
                    }
                });
            case 1 -> {
                String word = words.get(0);
                markov.generateNabeAsync(word).thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        ev.getChannel().sendMessage(maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know nothing about '" + word + "'");
                    }
                });
            }
            default -> {
                Collections.shuffle(words, ThreadLocalRandom.current());
                markov.generateFirstPossibleNabeAsync(words).thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        ev.getChannel().sendMessage(maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know nothing about '" + query + "'");
                    }
                });
            }
        }
    }
}
