package com.rrat.ogey.listeners.impl;

import com.rrat.ogey.components.MarkovModelComponent;
import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.model.AnnotateTokenizer;
import com.rrat.ogey.model.AnnotatedToken;
import com.rrat.ogey.model.AnnotatedTokens;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class NabeFactsCommandExecutor implements CommandExecutor, MessageCreateListener {

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

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        MessageAuthor author = ev.getMessageAuthor();
        if (!author.isBotUser()) {
            String content = ev.getMessageContent();
            if (!content.startsWith("!")) {
                AnnotateTokenizer tokenizer = AnnotateTokenizer.instance();
                markov.updateModelAsync(convertTokens(ev, tokenizer.tokenize(content)));
            }
        }
    }

    /**
     * Remove unknown emotes, replace user mentions with usernames
     */
    private List<String> convertTokens(MessageCreateEvent ev, List<AnnotatedToken> sequence) {

        DiscordApi discord = ev.getApi();

        final Map<Long, User> users = new HashMap<>();
        return sequence.stream()
                .filter(token -> token.handle(new AnnotatedToken.Handler<>() {
                    @Override
                    public Boolean onNewlineToken() {
                        return true;
                    }
                    @Override
                    public Boolean onWordToken(String word) {
                        return true;
                    }
                    @Override
                    public Boolean onPunctuationToken(String text) {
                        return true;
                    }
                    @Override
                    public Boolean onEmoteToken(String text, String name, long id) {
                        return discord.getCustomEmojiById(id).isPresent();
                    }

                    @Override
                    public Boolean onMentionToken(long id) {
                        CompletableFuture<User> future = discord.getUserById(id);
                        Optional<User> mentioned = future.handle((user, throwable) -> {
                            if (throwable != null) {
                                return Optional.<User>empty();
                            } else {
                                return Optional.of(user);
                            }
                        }).join();
                        mentioned.ifPresent(user -> users.put(id, user));
                        return mentioned.isPresent();
                    }
                }))
                .map(token -> token.handle(new AnnotatedToken.Handler<String>() {
                    @Override
                    public String onNewlineToken() {
                        return token.asText();
                    }
                    @Override
                    public String onWordToken(String word) {
                        return token.asText();
                    }
                    @Override
                    public String onPunctuationToken(String text) {
                        return token.asText();
                    }
                    @Override
                    public String onEmoteToken(String text, String name, long id) {
                        return token.asText();
                    }
                    @Override
                    public String onMentionToken(long id) {
                        User user = users.get(id);
                        return user.getName();
                    }
                }))
                .toList();
    }
}