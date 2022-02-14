package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.components.MarkovModelComponent;
import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.model.AnnotateTokenizer;
import com.rrat.ogey.model.AnnotatedToken;
import com.rrat.ogey.model.AnnotatedTokens;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class FactsCommandExecutor implements CommandExecutor, MessageCreateListener {

    private final AnnotateTokenizer tokenizer = AnnotateTokenizer.create();

    @Autowired
    private MarkovModelComponent markov;

    @Override
    public void execute(MessageCreateEvent ev, String arguments) {
        if (arguments != null) {
            spitFacts(ev, AnnotatedTokens.listWords(tokenizer.tokenize(arguments)), arguments);
        } else {
            spitFacts(ev, Collections.emptyList(), null);
        }
    }

    private void spitFacts(MessageCreateEvent ev, List<String> words, String query) {
        switch (words.size()) {
            case 0 -> {
                markov.generateSentenceAsync().thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        ev.getChannel().sendMessage(maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know no facts yet");
                    }
                });
            }
            case 1 -> {
                String word = words.get(0);
                markov.generateSentenceAsync(word).thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        ev.getChannel().sendMessage(maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know nothing about '" + word + "'");
                    }
                });
            }
            default -> {
                Collections.shuffle(words, ThreadLocalRandom.current());
                markov.generateFirstPossibleSentenceAsync(words).thenAccept(maybeSentence -> {
                    if (maybeSentence.isPresent()) {
                        ev.getChannel().sendMessage(maybeSentence.get());
                    } else {
                        ev.getChannel().sendMessage("I know nothing about '" + query + "'");
                    }
                });
            }
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        MessageAuthor author = ev.getMessageAuthor();
        if (!author.isBotUser()) {
            String content = ev.getMessageContent();
            if (!content.startsWith("!")) {
                List<AnnotatedToken> sequence = filterTokens(ev.getApi(), tokenizer.tokenize(content));
                markov.updateModelAsync(AnnotatedTokens.listAsText(sequence));
            }
        }
    }

    /** Remove emote unknown to bot because it will not be able to replicate them */
    private List<AnnotatedToken> filterTokens(DiscordApi discord, List<AnnotatedToken> sequence) {
        return sequence.stream()
                .filter(token -> token.handle(new AnnotatedToken.Handler<Boolean>() {
                    @Override public Boolean onNewlineToken() {
                        return true;
                    }
                    @Override public Boolean onWordToken(String word) {
                        return true;
                    }
                    @Override public Boolean onPunctuationToken(String text) {
                        return true;
                    }
                    @Override public Boolean onEmoteToken(String text, String name, long id) {
                        return discord.getCustomEmojiById(id).isPresent();
                    }
                }))
                .toList();
    }
}
