package com.rrat.ogey.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

public final class MarkovModel implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public static MarkovModel withNGramLength(int length) {
        return new MarkovModel(length);
    }

    public void update(List<String> tokens) {
        if (tokens.size() > ngramLength) {

            final String[] first = new String[ngramLength];
            for (int j = 0; j < ngramLength; j++) {
                first[j] = tokens.get(j);
            }

            NGram cursor = new NGram(first);
            for (int j = ngramLength; j < tokens.size(); j++) {
                String token = tokens.get(j);
                update(cursor, token);
                cursor = NGram.shift(cursor, token);
            }

            // Null-outcome indicates the end of a post:
            update(cursor, null);
        }
    }

    public Optional<List<String>> generate(RandomGenerator rng) {
        return pickNGram(rng)
                .map(ngram -> generate(rng, ngram));
    }

    public Optional<List<String>> generate(String token, RandomGenerator rng) {
        return pickNGram(token, rng)
                .map(ngram -> generate(rng, ngram));
    }

    private List<String> generate(RandomGenerator rng, NGram start) {

        ArrayList<String> tokens = new ArrayList<>();
        Collections.addAll(tokens, start.tokens);

        NGram cursor = start;
        while (true) {
            String outcome = pickOutcome(cursor, rng);
            if (outcome == null) {
                return tokens;
            } else {
                tokens.add(outcome);
                cursor = NGram.shift(cursor, outcome);
            }
        }
    }

    private void update(NGram ngram, String outcome) {
        NGramCounter counter = counters.computeIfAbsent(ngram, k -> {
            if (ngram.word) {
                prefixes.computeIfAbsent(ngram.tokens[0], _k -> new ArrayList<>()).add(ngram);
                wordNgrams++;
            }
            return new NGramCounter(ngram);
        });
        counter.outcomes.compute(outcome, (k, v) -> {
            if (v == null) {
                return 1;
            } else {
                return 1 + v;
            }
        });
        counter.occurrences++;
    }

    private Optional<NGram> pickNGram(RandomGenerator rng) {
        if (wordNgrams <= 0) {
            return Optional.empty();
        } else {
            Set<NGram> ngrams = counters.keySet();
            return ngrams.stream()
                    .filter(ng -> ng.word)
                    .skip(rng.nextInt(wordNgrams))
                    .findFirst();
        }
    }

    private Optional<NGram> pickNGram(String token, RandomGenerator rng) {
        return Optional.ofNullable(prefixes.get(token))
                .map(options -> options.get(rng.nextInt(options.size())));
    }

    private String pickOutcome(NGram ngram, RandomGenerator rng) {
        NGramCounter counter = counters.get(ngram);
        if (counter != null) {
            int pick = rng.nextInt(counter.occurrences);
            for (Map.Entry<String, Integer> item : counter.outcomes.entrySet()) {
                String outcome = item.getKey();
                int frequency = item.getValue();
                if (pick < frequency) {
                    return outcome;
                } else {
                    pick -= frequency;
                }
            }
        }
        return null;
    }

    private final int ngramLength;

    /** Use LinkedHashMap to preserve iteration order and yield consistent random picks */
    private final LinkedHashMap<NGram, NGramCounter> counters = new LinkedHashMap<>();
    private final HashMap<String, List<NGram>> prefixes = new HashMap<>();
    private int wordNgrams = 0;

    private MarkovModel(int ngramLength) {
        this.ngramLength = ngramLength;
    }

    private final static class NGram implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        private static final Pattern punctuation = Pattern.compile("^[\\n\\p{Punct}]$");

        private final int hash;
        private final String[] tokens;

        /** Indicates whether ngram starts with a word token */
        private final boolean word;

        NGram(String[] tokens) {
            String first = tokens[0];
            this.tokens = tokens.clone();
            this.hash = Arrays.hashCode(tokens);
            this.word = !punctuation.matcher(first).matches();
        }

        static NGram shift(NGram that, String last) {
            String[] tokens = that.tokens.clone();
            for (int j = 0; 1 + j < tokens.length; j++) {
                tokens[j] = tokens[1 + j];
            }
            tokens[tokens.length - 1] = last;
            return new NGram(tokens);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else if (obj == this) {
                return true;
            } else if (NGram.class.equals(obj.getClass())) {
                NGram that = (NGram) obj;
                return Arrays.equals(this.tokens, that.tokens);
            } else {
                return false;
            }
        }
    }

    private final static class NGramCounter implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        private final NGram self;
        private final LinkedHashMap<String, Integer> outcomes = new LinkedHashMap<>(2);
        private int occurrences = 0;

        private NGramCounter(NGram self) {
            this.self = self;
        }
    }
}
