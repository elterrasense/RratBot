package com.rrat.ogey.components;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.model.MarkovModel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Component
public class MarkovModelComponent implements MessageCreateListener, CommandExecutor {

    private static final Pattern pt_token_split = Pattern.compile("\\b");
    private static final Pattern pt_word = Pattern.compile("\\w+");
    private static final Pattern pt_punctuation = Pattern.compile("^[.,;:!?']$");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MarkovModel model = MarkovModel.withNGramLength(2);

    @PreDestroy
    private void preDestroy() {
        executor.shutdownNow();
    }

    @Override
    public void execute(MessageCreateEvent ev, String arguments) {
        if (arguments != null) {
            List<String> args = tokenize(arguments);
            switch (args.size()) {
                case 0 -> {
                    spitFacts(ev, null);
                }
                case 1 -> {
                    spitFacts(ev, args.get(0));
                }
                default -> {
                    ev.getChannel().sendMessage("You are asking a lot.");
                }
            }
        } else {
            spitFacts(ev, null);
        }
    }

    private void spitFacts(MessageCreateEvent ev, String token) {
        executor.execute(() -> {
            final Optional<List<String>> result;
            if (token != null) {
                result = model.generate(token, ThreadLocalRandom.current());
            } else {
                result = model.generate(ThreadLocalRandom.current());
            }
            if (result.isPresent()) {
                ev.getChannel().sendMessage(gather(result.get()));
            } else if (token != null) {
                ev.getChannel().sendMessage("I know nothing about '" + token + "'");
            } else {
                ev.getChannel().sendMessage("I know no facts yet");
            }
        });
    }

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        MessageAuthor author = ev.getMessageAuthor();
        if (!author.isBotUser()) {
            String context = ev.getMessageContent();
            executor.execute(() ->
                    model.update(tokenize(context)));
        }
    }

    private List<String> tokenize(String text) {
        List<String> tokens =
                pt_token_split.splitAsStream(sanitize(text))
                        .map(String::trim)
                        .filter(item -> !item.isEmpty())
                        .filter(item -> pt_word.matcher(item).matches() || pt_punctuation.matcher(item).matches())
                        .toList();

        return tokenizeEmotes(tokens);
    }

    /** Primarily remove urls from text since it will mess up the tokenizer */
    private String sanitize(String text) {
        return text.replaceAll("http.*?\\s", "");
    }

    /** Detect ':', 'identifier', ':' token sequence and replace it with single emote token */
    private List<String> tokenizeEmotes(List<String> tokens) {
        ArrayList<String> condensed = new ArrayList<>();
        for (int j = 0; j < tokens.size(); j++) {
            if (isEmoteTokenSequence(tokens, j)) {
                condensed.add(":" + tokens.get(1 + j) + ":");
                j = j + 2;
            } else {
                condensed.add(tokens.get(j));
            }
        }
        return condensed;
    }

    private boolean isEmoteTokenSequence(List<String> tokens, int position) {
        return 2 + position < tokens.size()
            && ":".equals(tokens.get(position))
            && ":".equals(tokens.get(2 + position));
    }

    private String gather(List<String> tokens) {
        StringBuilder text = new StringBuilder();
        for (int j = 0; j < tokens.size(); j++) {
            String token = tokens.get(j);
            if (j == 0) {
                text.append(token);
            } else if (pt_punctuation.matcher(token).matches()) {
                text.append(token);
                if ("'".equals(token) && 1 + j < tokens.size()) {
                    text.append(tokens.get(1 + j));
                    j++;
                }
            } else {
                text.append(' ').append(token);
            }
        }
        return text.toString();
    }
}
