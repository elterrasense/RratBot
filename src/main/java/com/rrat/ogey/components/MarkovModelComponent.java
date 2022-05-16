package com.rrat.ogey.components;

import com.rrat.ogey.model.AnnotateTokenizer;
import com.rrat.ogey.model.MarkovModel;
import com.rrat.ogey.model.MarkovModels;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Component
public class MarkovModelComponent {

    private static final Logger logger = LoggerFactory.getLogger(MarkovModelComponent.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private MarkovModel model;
    private MarkovModel nabe;

    @Autowired
    private Environment env;

    @PostConstruct
    private void postConstruct() {

        this.nabe = MarkovModels.loadResource("/.nabe-chain.obj");

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

    public void updateModelAsync(List<String> textTokens) {
        executor.execute(() -> model.update(textTokens));
    }

    public CompletableFuture<Optional<String>> generateNabeAsync() {
        Supplier<Optional<String>> supplier = () ->
                Optional.ofNullable(nabe)
                        .flatMap(m -> m.generate(ThreadLocalRandom.current()))
                        .map(this::gather);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Optional<String>> generateFirstPossibleNabeAsync(List<String> words) {
        Supplier<Optional<String>> supplier = () -> {
            for (String word : words) {
                Optional<List<String>> result =  Optional.ofNullable(nabe)
                        .flatMap(m -> m.generate(word,ThreadLocalRandom.current()));
                if (result.isPresent()) {
                    return result.map(this::gather);
                }
            }
            return Optional.empty();
        };
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Optional<String>> generateNabeAsync(String word) {
        Supplier<Optional<String>> supplier = () ->
                Optional.ofNullable(nabe)
                        .flatMap(m -> m.generate(word,ThreadLocalRandom.current()))
                        .map(this::gather);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Optional<String>> generateSentenceAsync() {
        Supplier<Optional<String>> supplier = () ->
                model.generate(ThreadLocalRandom.current()).map(this::gather);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Optional<String>> generateSentenceAsync(String word) {
        Supplier<Optional<String>> supplier = () ->
                model.generate(word, ThreadLocalRandom.current()).map(this::gather);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Optional<String>> generateFirstPossibleSentenceAsync(List<String> words) {
        Supplier<Optional<String>> supplier = () -> {
            for (String word : words) {
                Optional<List<String>> result = model.generate(word, ThreadLocalRandom.current());
                if (result.isPresent()) {
                    return result.map(this::gather);
                }
            }
            return Optional.empty();
        };
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Optional<String>> generateQuote() {
        Supplier<Optional<String>> supplier = () ->
                model.generateQuote(ThreadLocalRandom.current()).map(this::gather);
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    private @Nullable MarkovModel loadPersistentMarkov() {
        Path file = getModelObjectFilepath();
        try {
            return MarkovModels.load(file);
        } catch (RuntimeException exception) {
            logger.error(String.format("Unable to load persistent Markov chain '%s'", file), exception);
            return null;
        }
    }

    private void savePersistentMarkov() {
        Path file = getModelObjectFilepath();
        try {
            MarkovModels.save(file, model);
        } catch (RuntimeException exception) {
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
                            String next = tokens.get(1 + j);
                            if (">".equals(next)) {
                                text.append("\n>");
                                if (hasMore(tokens, 1 + j)) {
                                    text.append(tokens.get(2 + j));
                                    j = j + 2;
                                } else {
                                    j++;
                                }
                            } else {
                                text.append("\n");
                                text.append(next);
                                j++;
                            }
                        }
                    }
                    default -> text.append(token);

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
