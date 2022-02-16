package com.rrat.ogey.model;

import java.util.Objects;

public abstract class AnnotatedToken {
    private AnnotatedToken() {}

    public interface Handler<O> {
        O onNewlineToken();
        O onWordToken(String word);
        O onPunctuationToken(String text);
        O onEmoteToken(String text, String name, long id);
        O onMentionToken(long id);
    }

    public abstract String asText();
    public abstract <O> O handle(Handler<O> handler);
    public @Override abstract String toString();

    public static AnnotatedToken newline() {
        return NewlineTokenCase.instance;
    }

    public static AnnotatedToken word(String word) {
        return new WordTokenCase(Objects.requireNonNull(word));
    }

    public static AnnotatedToken punctuation(String punctuation) {
        return new PunctuationTokenCase(Objects.requireNonNull(punctuation));
    }

    public static AnnotatedToken mention(String text, long id) {
        return new MentionTokenCase(
                Objects.requireNonNull(text),
                id);
    }

    public static AnnotatedToken emote(String text, String name, long id) {
        return new EmoteTokenCase(
                Objects.requireNonNull(text),
                Objects.requireNonNull(name),
                id);
    }

    private static final class NewlineTokenCase extends AnnotatedToken {
        private static final NewlineTokenCase instance = new NewlineTokenCase();
        private NewlineTokenCase() {}
        @Override public <O> O handle(Handler<O> handler) {
            return handler.onNewlineToken();
        }
        @Override public String asText() {
            return "\n";
        }
        @Override public String toString() {
            return "AnnotatedToken.newline[]";
        }
    }

    private static final class WordTokenCase extends AnnotatedToken {
        private final String word;
        private WordTokenCase(String word) {
            this.word = word.toLowerCase();
        }
        @Override public String asText() {
            return word;
        }
        @Override public <O> O handle(Handler<O> handler) {
            return handler.onWordToken(word);
        }
        @Override public String toString() {
            return "AnnotatedToken.word[" + word + "]";
        }
    }

    private static final class PunctuationTokenCase extends AnnotatedToken {
        private final String punctuation;
        private PunctuationTokenCase(String punctuation) {
            this.punctuation = punctuation;
        }
        @Override public String asText() {
            return punctuation;
        }
        @Override public <O> O handle(Handler<O> handler) {
            return handler.onPunctuationToken(punctuation);
        }
        @Override public String toString() {
            return "AnnotatedToken.punctuation[" + punctuation + "]";
        }
    }

    private static final class EmoteTokenCase extends AnnotatedToken {
        private final String text;
        private final String emoteName;
        private final long emoteId;
        private EmoteTokenCase(String text, String emoteName, long emoteId) {
            this.emoteId = emoteId;
            this.emoteName = emoteName;
            this.text = text;
        }
        @Override public String asText() {
            return text;
        }
        @Override public <O> O handle(Handler<O> handler) {
            return handler.onEmoteToken(text, emoteName, emoteId);
        }
        @Override public String toString() {
            return "AnnotatedToken.emote[" + emoteName + ", " + emoteId + "]";
        }
    }

    private static final class MentionTokenCase extends AnnotatedToken {
        private final String text;
        private final long id;
        private MentionTokenCase(String text, long id) {
            this.text = text;
            this.id = id;
        }
        @Override public <O> O handle(Handler<O> handler) {
            return handler.onMentionToken(id);
        }
        @Override public String asText() {
            return text;
        }
        @Override public String toString() {
            return "AnnotatedToken.mention[" + id  + "]";
        }
    }
}
