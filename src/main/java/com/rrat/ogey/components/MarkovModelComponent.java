package com.rrat.ogey.components;

import com.rrat.ogey.listeners.CommandExecutor;
import com.rrat.ogey.model.AnnotateTokenizer;
import com.rrat.ogey.model.AnnotatedToken;
import com.rrat.ogey.model.AnnotatedTokens;
import com.rrat.ogey.model.MarkovModel;
import org.javacord.api.DiscordApi;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

@Component
public class MarkovModelComponent implements MessageCreateListener, CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MarkovModelComponent.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AnnotateTokenizer tokenizer = AnnotateTokenizer.create();

    private MarkovModel model;

    @Autowired
    private Environment env;

    @PostConstruct
    private void postConstruct() {

        this.model = Objects.requireNonNullElseGet(
                loadPersistentMarkov(),
                () -> MarkovModel.withNGramLength(2));

        this.scheduler.scheduleAtFixedRate(
                () -> executor.execute(this::savePersistentMarkov),
                5,
                5, TimeUnit.MINUTES);
    }

    @PreDestroy
    private void preDestroy() {
        executor.shutdownNow();
        scheduler.shutdownNow();
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
            spitFacts(ev, AnnotatedTokens.listWords(tokenizer.tokenize(arguments)), arguments);
        } else {
            spitFacts(ev, Collections.emptyList(), null);
        }
    }

    private void spitFacts(MessageCreateEvent ev, List<String> words, String query) {
        switch (words.size()) {
            case 0 -> executor.execute(() -> {
                Optional<List<String>> result = model.generate(ThreadLocalRandom.current());
                if (result.isPresent()) {
                    ev.getChannel().sendMessage(gather(result.get()));
                } else {
                    ev.getChannel().sendMessage("I know no facts yet");
                }
            });
            case 1 -> executor.execute(() -> {
                String word = words.get(0);
                Optional<List<String>> result = model.generate(word, ThreadLocalRandom.current());
                if (result.isPresent()) {
                    ev.getChannel().sendMessage(gather(result.get()));
                } else {
                    ev.getChannel().sendMessage("I know nothing about '" + word + "'");
                }
            });
            default -> executor.execute(() -> {
                Collections.shuffle(words, ThreadLocalRandom.current());
                for (String word : words) {
                    Optional<List<String>> result = model.generate(word, ThreadLocalRandom.current());
                    if (result.isPresent()) {
                        ev.getChannel().sendMessage(gather(result.get()));
                        return;
                    }
                }
                ev.getChannel().sendMessage("I know nothing about '" + query + "'");
            });
        }
    }

    @Override
    public void onMessageCreate(MessageCreateEvent ev) {
        MessageAuthor author = ev.getMessageAuthor();
        if (!author.isBotUser()) {
            String content = ev.getMessageContent();
            if (!content.startsWith("!")) {
                List<AnnotatedToken> sequence = filterTokens(ev.getApi(), tokenizer.tokenize(content));
                executor.execute(() -> model.update(AnnotatedTokens.listAsText(sequence)));
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

    /**
     * Builds a post from text tokens, primarily follows these rules:
     * word should have a space before it with an exception to {@code '}, {@code >}, and newline characters;
     * {@code >} character always is preceded by newline to replicate green-text posts
     */
    private String gather(List<String> tokens) {
        StringBuilder text = new StringBuilder();
        for (int j = 0; j < tokens.size(); j++) {
            String token = tokens.get(j);
            if (j == 0) {
                text.append(token);
                if (">".equals(token) && hasMore(tokens, j)) {
                    text.append(tokens.get(1 + j));
                    j++;
                }
            } else if (AnnotateTokenizer.pt_punctuation.matcher(token).matches()) {
                switch (token) {
                    case "'" -> {
                        text.append("'");
                        if (hasMore(tokens, j)) {
                            text.append(tokens.get(1 + j));
                            j++;
                        }
                    }
                    case ">" -> {
                        if (hasMore(tokens, j)) {
                            text.append("\n>");
                            text.append(tokens.get(1 + j));
                            j++;
                        }
                    }
                    case "\n" -> {
                        if (hasMore(tokens, j)) {
                            text.append("\n");
                            text.append(tokens.get(1 + j));
                            j++;
                        }
                    }
                    default -> {
                        text.append(token);
                    }
                }
            } else {
                text.append(' ').append(token);
            }
        }
        return text.toString();
    }

    private boolean hasMore(List<String> tokens, int j) {
        return 1 + j < tokens.size();
    }
}
