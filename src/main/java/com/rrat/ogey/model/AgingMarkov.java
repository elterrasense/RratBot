package com.rrat.ogey.model;

import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Modification of {@link MarkovModel} with support of aging links within the graph.
 * It should forget and remove n-grams inserted after a certain date.
 */
public final class AgingMarkov implements GenericMarkov {
    @Serial private static final long serialVersionUID = 3L;

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
            Node found = gram.get(source);
            if (found == null) {
                node = new Node(now, source);
                nodes.put(node.id, node);
                gram.put(source, node);
            } else {
                node = found;
                found.updated = now;
            }
        }
        if (target == null) {
            Link link = new Link(now, null);
            links.put(link.id, link);
            node.linkIds.addLast(link.id);
        } else {
            final Node t; {
                Node found = gram.get(target);
                if (found == null) {
                    t = new Node(now, target);
                    nodes.put(t.id, t);
                    gram.put(target, t);
                } else {
                    t = found;
                    t.updated = now;
                }
            }
            Link link = new Link(now, t.id);
            links.put(link.id, link);
            node.linkIds.addLast(link.id);
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
        List<Node> pick = nodes.values()
                .stream()
                .filter(node -> predicate.test(node.self))
                .toList();

        if (pick.isEmpty()) return Optional.empty();
        Node picked = pick.get(rng.nextInt(pick.size()));
        return Optional.of(picked.self);
    }

    private List<String> traverse(RandomGenerator rng, NGram start) {
        ArrayList<String> tokens = new ArrayList<>();
        Collections.addAll(tokens, start.tokens);
        Node node = gram.get(start);
        while (true) {
            if (node == null) {
                return tokens;
            }
            if (node.linkIds.isEmpty()) {
                return tokens;
            }
            UUID lid = node.linkIds.get(rng.nextInt(node.linkIds.size()));
            Link link = links.get(lid);
            if (link == null) {
                return tokens;
            }
            UUID nid = link.targetNodeId;
            node = nodes.get(nid);
            if (node != null) {
                tokens.add(node.self.tokens[node.self.tokens.length - 1]);
            }
        }
    }

    /** Removes links and nodes older than retain date. */
    public void prune(Instant retain) {
        { // Prune links;
            Collection<UUID> ids = new ArrayList<>(links.keySet());
            for (UUID lid : ids) {
                Link link = links.get(lid);
                if (link.ts.isBefore(retain)) {
                    links.remove(lid);
                }
            }
        }
        { // Prune nodes;
            Collection<UUID> ids = new ArrayList<>(nodes.keySet());
            for (UUID nid : ids) {
                Node node = nodes.get(nid);
                node.linkIds.removeIf(id -> !links.containsKey(id));
                if (node.updated.isBefore(retain)) {
                    nodes.remove(nid);
                }
            }
        }
        { // Prune nGram map;
            Collection<Node> cached = new ArrayList<>(gram.values());
            for (Node node : cached) {
                if (!nodes.containsKey(node.id)) {
                    gram.remove(node.self);
                }
            }
        }
    }

    private final int ngramLength;
    private final HashMap<UUID, Link> links = new HashMap<>();
    private final HashMap<UUID, Node> nodes = new HashMap<>();
    private final HashMap<NGram, Node> gram = new HashMap<>();

    private static final class Link implements Serializable {
        @Serial private static final long serialVersionUID = 3L;
        private final Instant ts;
        private final @Nullable UUID targetNodeId;
        private final UUID id = UUID.randomUUID();
        private Link(Instant ts, @Nullable UUID targetNodeId) {
            this.ts = ts;
            this.targetNodeId = targetNodeId;
        }
    }

    private static final class Node implements Serializable {
        @Serial private static final long serialVersionUID = 3L;
        private Instant updated;
        private final NGram self;
        private final LinkedList<UUID> linkIds = new LinkedList<>();
        private final UUID id = UUID.randomUUID();
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

    private AgingMarkov(int length) {
        this.ngramLength = length;
    }
}
