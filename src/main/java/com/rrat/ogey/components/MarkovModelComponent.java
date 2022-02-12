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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

@Component
public class MarkovModelComponent implements MessageCreateListener, CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MarkovModelComponent.class);

    private static final Pattern pt_token_split = Pattern.compile("\\b");
    private static final Pattern pt_word = Pattern.compile("\\w+");
    private static final Pattern pt_punctuation = Pattern.compile("^[>.,;:!?'\n]$");

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
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
            List<String> args = tokenize(arguments)
                    .stream()
                    .filter(pt_word.asPredicate())
                    .toList();
            switch (args.size()) {
                case 0 -> {
                    spitFacts(ev, null);
                }
                case 1 -> {
                    spitFacts(ev, args.get(0));
                }
                default -> {
                    RandomGenerator rng = ThreadLocalRandom.current();
                    spitFacts(ev, args.get(rng.nextInt(args.size())));
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
            if (!content.startsWith("!")) {
                executor.execute(() -> model.update(tokenize(content)));
            }
        }
    }

    private List<String> tokenize(String text) {
        List<String> tokens =
                pt_token_split.splitAsStream(sanitize(text))
                        .map(item -> {
                            if (item.contains("\n")) {
                                return "\n";
                            } else {
                                return item.trim();
                            }
                        })
                        .filter(item -> !item.isEmpty())
                        .toList();

        return filterTokens(tokens);
    }

    /** Primarily remove urls from text since it will mess up the tokenizer */
    private String sanitize(String text) {
        return text.replaceAll("http.*?(\\s|$)", "");
    }

    /**
     * Find emote token sequences and replace them with a single emote token.
     * Also remove non-word tokens as well as not-supported punctuation tokens.
     */
    private List<String> filterTokens(List<String> tokens) {
        ArrayList<String> filtered = new ArrayList<>();
        for (int j = 0; j < tokens.size(); j++) {
            if (isEmoteTokenSequence(tokens, j)) {
                String emoteName = tokens.get(1 + j);
                String emoteId = tokens.get(3 + j);
                filtered.add("<:" + emoteName + ":" + emoteId + ">");
                j = j + 4;
            } else {
                String token = tokens.get(j);
                if (pt_word.matcher(token).matches() || pt_punctuation.matcher(token).matches()) {
                    filtered.add(token.toLowerCase());
                }
            }
        }
        return filtered;
    }

    private boolean isEmoteTokenSequence(List<String> tokens, int position) {
        return 4 + position < tokens.size()
            && "<:".equals(tokens.get(position))
            && ":".equals(tokens.get(2 + position))
            && ">".equals(tokens.get(4 + position))
            && pt_word.matcher(tokens.get(1 + position)).matches()
            && Pattern.matches("\\d+", tokens.get(3 + position));
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
            } else if (pt_punctuation.matcher(token).matches()) {
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
