package com.rrat.ogey.model;

import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Modification of {@link MarkovModel} with support of aging links within the graph.
 * It should forget and remove n-grams inserted after a certain date.
 */
public final class AgingMarkov implements GenericMarkov {
    @Serial private static final long serialVersionUID = 2L;

    public static AgingMarkov withNGramLength(int length) {
        return new AgingMarkov(length);
    }

    @Override
    public void update(List<String> token) {
        update(Instant.now(), token);
    }

    /** @param now date used to mark an insertion, swiping algorithm then forgets old links based on retain date */
    public void update(Instant now, List<String> tokens) {
        if (tokens.size() > ngramLength) {
            final String[] first = new String[ngramLength];
            for (int j = 0; j < ngramLength; j++) {
                first[j] = tokens.get(j);
            }
            NGram cursor = new NGram(first);
            for (int j = ngramLength; j < tokens.size(); j++) {
                NGram next = NGram.shift(cursor, tokens.get(j));
                insert(now, cursor, next);
                cursor = next;
            }
            insert(now, cursor, null);
        }
    }

    private void insert(Instant now, NGram source, @Nullable NGram target) {
        final Node node; {
            Node found = nodes.get(source);
            if (found == null) {
                node = new Node(now, source);
                nodes.put(source, node);
            } else {
                node = found;
                found.updated = now;
            }
        }
        if (target == null) {
            node.links.addLast(new Link(now, null));
        } else {
            final Node t;
            {
                Node found = nodes.get(target);
                if (found == null) {
                    t = new Node(now, target);
                    nodes.put(target, t);
                } else {
                    t = found;
                    t.updated = now;
                }
            }
            node.links.addLast(new Link(now, t));
        }
    }

    @Override
    public Optional<List<String>> generate(RandomGenerator rng) {
        return pick(rng, ng -> ng.wordTokenFirst).map(start -> traverse(rng, start));
    }

    @Override
    public Optional<List<String>> generate(String token, RandomGenerator rng) {
        return pick(rng, ng -> Objects.equals(token, ng.tokens[0])).map(start -> traverse(rng, start));
    }

    private Optional<NGram> pick(RandomGenerator rng, Predicate<NGram> predicate) {
        if (nodes.isEmpty()) return Optional.empty();
        if (nodes.keySet().stream().anyMatch(predicate)) {
            return Stream.generate(nodes::keySet)
                    .flatMap(Collection::stream)
                    .filter(predicate)
                    .skip(rng.nextInt(nodes.size())) // This introduces modulo-bias. Unfortunately I don't care.
                    .findAny();
        } else {
            return Optional.empty();
        }
    }

    private List<String> traverse(RandomGenerator rng, NGram start) {
        ArrayList<String> tokens = new ArrayList<>();
        Collections.addAll(tokens, start.tokens);
        Node node = nodes.get(start);
        while (true) {
            if (node == null) {
                return tokens;
            }
            if (node.links.isEmpty()) {
                return tokens;
            }
            Link link = node.links.get(rng.nextInt(node.links.size()));
            node = link.target;
            if (node != null) {
                tokens.add(node.self.tokens[node.self.tokens.length - 1]);
            }
        }
    }

    /** Removes links and nodes older than retain date. */
    public void prune(Instant retain) {
        Collection<NGram> ngrams = new ArrayList<>(nodes.keySet());
        for (NGram ngram : ngrams) {
            Node node = nodes.get(ngram);
            node.links.removeIf(link -> link.ts.isBefore(retain));
            if (node.updated.isBefore(retain)) {
                nodes.remove(ngram);
            }
        }
    }

    private final int ngramLength;
    private final HashMap<NGram, Node> nodes = new HashMap<>();

    private AgingMarkov(int length) {
        this.ngramLength = length;
    }

    private static final class Link implements Serializable {
        @Serial private static final long serialVersionUID = 2L;
        private final Instant ts;
        private final @Nullable Node target;
        private Link(Instant ts, @Nullable Node target) {
            this.ts = ts;
            this.target = target;
        }
    }

    private static final class Node implements Serializable {
        @Serial private static final long serialVersionUID = 2L;
        private Instant updated;
        private final NGram self;
        private final LinkedList<Link> links = new LinkedList<>();
        private Node(Instant updated, NGram self) {
            this.updated = updated;
            this.self = self;
        }
    }

    private static final class NGram implements Serializable {
        @Serial private static final long serialVersionUID = 2L;
        private static final Pattern punctuation = Pattern.compile("^[\\n\\p{Punct}]$");
        private final int hash;
        private final String[] tokens;
        private final boolean wordTokenFirst;
        NGram(String[] tokens) {
            String first = tokens[0];
            this.tokens = tokens.clone();
            this.hash = Arrays.hashCode(tokens);
            this.wordTokenFirst = !punctuation.matcher(first).matches();
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

}
