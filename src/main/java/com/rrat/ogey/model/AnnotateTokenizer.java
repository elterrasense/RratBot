package com.rrat.ogey.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Dedicated text tokenizer operation that processes text in multiple passes.
 *
 * See {@link AnnotatedToken} for recognized text token types, currently only text and emote tokens are supported.
 * Intended to fix inconsistency of simple split operation, as it is easier to detect emotes with regular expression
 * and then split remaining text manually.
 */
public final class AnnotateTokenizer {

    public static AnnotateTokenizer instance() {
        return AnnotateTokenizerHolder.instance;
    }

    /** g: initialization-on-demand holder idiom */
    private static final class AnnotateTokenizerHolder {
        private static final AnnotateTokenizer instance = new AnnotateTokenizer();
    }

    public static final Pattern pt_word_split = Pattern.compile("\\b");
    public static final Pattern pt_word = Pattern.compile("\\w+");
    public static final Pattern pt_punctuation = Pattern.compile("^[>.,;:!?'\n]$");

    public List<AnnotatedToken> tokenize(String text) {

        Stream<TokenizerSequence> sequence =
                Stream.<TokenizerSequence>of(new TokenizerSequence.UnprocessedText(text)).sequential();

        // Apply tokenizer passes in order defined by chain.
        for (TokenizerPass pass : chain) {
            sequence = sequence.flatMap(item -> item.handle(new TokenizerSequenceHandler<Stream<TokenizerSequence>>() {
                @Override public Stream<TokenizerSequence> onUnprocessedText(String text) {
                    return pass.process(text);
                }
                @Override public Stream<TokenizerSequence> onAnnotatedToken(AnnotatedToken token) {
                    return Stream.of(item);
                }
            }));
        }

        // Collected annotated tokens, discard any remaining unprocessed text.
        ArrayList<AnnotatedToken> tokens = new ArrayList<>();
        sequence.forEachOrdered(item -> {
            item.handle(new TokenizerSequenceHandler<Void>() {
                @Override public Void onUnprocessedText(String text) {
                    return null;
                }
                @Override public Void onAnnotatedToken(AnnotatedToken token) {
                    tokens.add(token);
                    return null;
                }
            });
        });
        return tokens;
    }

    private final List<TokenizerPass> chain;

    private AnnotateTokenizer() {
        chain = Arrays.asList(
                // Wipe hyperlinks from the post, otherwise it creates nonsensical text sequences later on
                hyperlink_removal_pass,
                // Locate and annotate user mentions
                mention_annotation_pass,
                // Locate and annotate discord emotes
                emote_annotation_pass,
                // Locate and annotate discord channels
                channel_annotation_pass,
                // Locate and annotate discord roles
                role_annotation_pass,
                // Split newlines and insert newline tokens
                newline_split_pass,
                // Locate words in the text and extract word tokens
                word_split_pass,
                // Extract supported punctuation tokens: periods, commas, etc
                tokenize_supported_punctuation);

    }

    private interface TokenizerPass {
        Stream<TokenizerSequence> process(String text);
    }

    private interface TokenizerSequenceHandler<O> {
        O onUnprocessedText(String text);
        O onAnnotatedToken(AnnotatedToken token);
    }

    private static abstract class TokenizerSequence {
        protected abstract <O> O handle(TokenizerSequenceHandler<O> handler);
        private static final class UnprocessedText extends TokenizerSequence {
            private final String text;
            private UnprocessedText(String text) {
                this.text = text;
            }
            @Override protected <O> O handle(TokenizerSequenceHandler<O> handler) {
                return handler.onUnprocessedText(text);
            }
        }
        private static final class Token extends TokenizerSequence {
            private final AnnotatedToken token;
            private Token(AnnotatedToken token) {
                this.token = token;
            }
            @Override protected <O> O handle(TokenizerSequenceHandler<O> handler) {
                return handler.onAnnotatedToken(token);
            }
        }
    }

    /**
     * Iterate over regexp matches while keeping track of surrounding text
     * @param action accepts text before last match until now and the matcher itself
     * @return remaining text after the last match
     */
    private static String forEachMatch(
            Pattern pattern,
            String text,
            BiConsumer<String, Matcher> action) {

        Matcher matcher = pattern.matcher(text);

        // Split string into text before the match, currently matched text, and remaining text.
        // index stores the location of remain substring in the original argument string,
        // since match refers to location in it, rather than in 'remain' string
        int index = 0;
        String remain = text;

        while (matcher.find()) {

            String unprocessed = remain.substring(0, matcher.start() - index);
            remain = remain.substring(matcher.end() - index, remain.length());
            index = matcher.end();

            action.accept(unprocessed, matcher);
        }

        return remain;
    }

    /** Automatically converts surrounding text into tokenizer sequence wrapper */
    private static Stream<TokenizerSequence> tokenizeByPattern(
            Pattern pattern,
            String text,
            Function<Matcher, TokenizerSequence> fn) {

        Stream.Builder<TokenizerSequence> sequence = Stream.builder();
        String remain = forEachMatch(pattern, text, (unprocessed, matcher) -> {
            sequence.add(new TokenizerSequence.UnprocessedText(unprocessed));
            sequence.add(fn.apply(matcher));
        });
        sequence.add((new TokenizerSequence.UnprocessedText(remain)));
        return sequence.build();
    }

    private static final TokenizerPass hyperlink_removal_pass = text ->
            Stream.of(new TokenizerSequence.UnprocessedText(text.replaceAll("http.*?(\\s|$)", "")));

    private static final Pattern pt_emote = Pattern.compile("<(a)?:(?<name>\\w+):(?<id>\\d+)>");
    private static final TokenizerPass emote_annotation_pass = text ->
            tokenizeByPattern(pt_emote, text, matcher ->
                    new TokenizerSequence.Token(
                            AnnotatedToken.emote(
                                    matcher.group(),
                                    matcher.group("name"),
                                    Long.parseLong(matcher.group("id")))));

    private static final Pattern pt_mention = Pattern.compile("<@(?<id>\\w+)>");
    private static final TokenizerPass mention_annotation_pass = text ->
            tokenizeByPattern(pt_mention, text, matcher ->
                    new TokenizerSequence.Token(
                            AnnotatedToken.mention(
                                    matcher.group(),
                                    Long.parseLong(matcher.group("id")))));

    private static final Pattern pt_channel = Pattern.compile("<#(?<id>\\d+)>");
    private static final TokenizerPass channel_annotation_pass = text ->
            tokenizeByPattern(pt_channel, text, matcher ->
                    new TokenizerSequence.Token(
                            AnnotatedToken.channel(
                                    matcher.group(),
                                    Long.parseLong(matcher.group("id")))));

    private static final Pattern pt_role = Pattern.compile("<@&(?<id>\\d+)>");
    private static final TokenizerPass role_annotation_pass = text ->
            tokenizeByPattern(pt_role, text, matcher ->
                    new TokenizerSequence.Token(
                            AnnotatedToken.role(
                                    matcher.group(),
                                    Long.parseLong(matcher.group("id")))));

    private static final TokenizerPass newline_split_pass = text -> {

        String[] lines = text.split("\n");
        Stream.Builder<TokenizerSequence> sequence = Stream.builder();

        for (int j = 0; j < lines.length; j++) {
            String line = lines[j].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (j != 0 ) {
                sequence.add(new TokenizerSequence.Token(AnnotatedToken.newline()));
            }
            sequence.add(new TokenizerSequence.UnprocessedText(lines[j]));
        }

        return sequence.build();
    };

    private static final TokenizerPass word_split_pass = text ->
            pt_word_split.splitAsStream(text)
                    .map(String::trim)
                    .map(item -> {
                        if (pt_word.matcher(item).matches()) {
                            return new TokenizerSequence.Token(AnnotatedToken.word(item));
                        } else {
                            return new TokenizerSequence.UnprocessedText(item);
                        }
                    });

    private static final TokenizerPass tokenize_supported_punctuation = text -> {
        if (pt_punctuation.matcher(text).matches()) {
            return Stream.of(new TokenizerSequence.Token(AnnotatedToken.punctuation(text)));
        } else {
            return Stream.of(new TokenizerSequence.UnprocessedText(text));
        }
    };
}
