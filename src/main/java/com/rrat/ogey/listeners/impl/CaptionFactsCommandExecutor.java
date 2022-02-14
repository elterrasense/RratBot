package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.components.MarkovModelComponent;
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
public class CaptionFactsCommandExecutor implements CommandExecutor {

    private final AnnotateTokenizer tokenizer = AnnotateTokenizer.create();

    @Autowired
    private MarkovModelComponent markov;

    @Override
    public void execute(MessageCreateEvent ev, String arguments) {
        if (arguments != null) {
            CaptionedFact(ev, AnnotatedTokens.listWords(tokenizer.tokenize(arguments)), arguments);
        } else {
            CaptionedFact(ev, Collections.emptyList(), null);
        }

    }

    private void CaptionedFact(MessageCreateEvent ev, List<String> words, String query) {
        switch (words.size()) {
            case 0 -> markov.generateSentenceAsync().thenAccept(maybeSentence -> {
                if (maybeSentence.isPresent()) {
                    new AddCaptionCommandExecutor().execute(ev, maybeSentence.get());
                } else {
                    ev.getChannel().sendMessage("I know no facts yet");
                }
            });
            case 1 -> {
                String word = words.get(0);
                markov.generateSentenceAsync(word).thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        new AddCaptionCommandExecutor().execute(ev, maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know nothing about '" + word + "'");
                    }
                });
            }
            default -> {
                Collections.shuffle(words, ThreadLocalRandom.current());
                markov.generateFirstPossibleSentenceAsync(words).thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        new AddCaptionCommandExecutor().execute(ev, maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know nothing about '" + query + "'");
                    }
                });
            }
        }
    }
}

