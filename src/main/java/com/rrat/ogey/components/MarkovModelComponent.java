package com.rrat.ogey.components;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.model.MarkovModel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Component
public class MarkovModelComponent implements MessageCreateListener, CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MarkovModelComponent.class);

    private static final Pattern pt_token_split = Pattern.compile("\\b");
    private static final Pattern pt_word = Pattern.compile("\\w+");
    private static final Pattern pt_punctuation = Pattern.compile("^[.,;:!?']$");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Instant lastWriteDate = Instant.now();
    private MarkovModel model;

    @Autowired
    private Environment env;

    @PostConstruct
    private void postConstruct() {
        this.model = Objects.requireNonNullElseGet(
                loadPersistentMarkov(),
                () -> MarkovModel.withNGramLength(2));
    }

    @PreDestroy
    private void preDestroy() {
        executor.shutdownNow();
    }

    private @Nullable MarkovModel loadPersistentMarkov() {
        Path file = getModelObjectFilepath();
        if (Files.exists(file)) {
            try (FileInputStream is = new FileInputStream(file.toFile())) {
                ObjectInputStream ois = new ObjectInputStream(is);
                return (MarkovModel) ois.readObject();
            } catch (ClassNotFoundException | IOException exception) {
                logger.error(String.format("Unable to load persistent Markov chain '%s'", file), exception);
                return null;
            }
        } else {
            return null;
        }
    }

    private void savePersistentMarkov() {
        Path file = getModelObjectFilepath();
        try (FileOutputStream os = new FileOutputStream(file.toFile())) {
            ObjectOutputStream ois = new ObjectOutputStream(os);
            ois.writeObject(model);
        } catch (IOException exception) {
            logger.error(String.format("Unable to save persistent Markov chain to '%s'", file), exception);
        }
    }

    private Path getModelObjectFilepath() {
        String customFilepath = env.getProperty("markov.persistentObjectFilepath");
        if (customFilepath != null) {
            return Paths.get(customFilepath);
        } else {
            return Paths.get(System.getProperty("user.home"), ".markov-chain.obj");
        }
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
            String content = ev.getMessageContent();
            executor.execute(() -> updateModel(content));
        }
    }

    private void updateModel(String text) {
        model.update(tokenize(text));
        Instant now = Instant.now();
        if (Duration.between(lastWriteDate, now).get(ChronoUnit.MINUTES) > 5) {
            lastWriteDate = now;
            savePersistentMarkov();
        }
    }

    private List<String> tokenize(String text) {
        List<String> tokens =
                pt_token_split.splitAsStream(sanitize(text))
                        .map(String::trim)
                        .filter(item -> !item.isEmpty())
                        .filter(item -> pt_word.matcher(item).matches() || pt_punctuation.matcher(item).matches())
                        .toList();

        return filterTokens(tokens);
    }

    /** Primarily remove urls from text since it will mess up the tokenizer */
    private String sanitize(String text) {
        return text.replaceAll("http.*?\\s", "");
    }

    /** Detect ':', 'identifier', ':' token sequence and replace it with single emote token, convert the rest to lowercase */
    private List<String> filterTokens(List<String> tokens) {
        ArrayList<String> condensed = new ArrayList<>();
        for (int j = 0; j < tokens.size(); j++) {
            if (isEmoteTokenSequence(tokens, j)) {
                condensed.add(":" + tokens.get(1 + j) + ":");
                j = j + 2;
            } else {
                condensed.add(tokens.get(j).toLowerCase());
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
