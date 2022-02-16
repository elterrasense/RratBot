package com.rrat.ogey.model;

import java.util.ArrayList;
import java.util.List;

public final class AnnotatedTokens {
    private AnnotatedTokens() {}

    public static List<String> listWords(List<AnnotatedToken> sequence) {
        ArrayList<String> words = new ArrayList<>(sequence.size());
        for (AnnotatedToken token : sequence) token.handle(new AnnotatedToken.Handler<Void>() {
            @Override public Void onNewlineToken() {
                return null;
            }
            @Override public Void onWordToken(String word) {
                words.add(word);
                return null;
            }
            @Override public Void onPunctuationToken(String text) {
                return null;
            }
            @Override public Void onEmoteToken(String text, String name, long id) {
                return null;
            }
            @Override public Void onMentionToken(long id) {
                return null;
            }
        });
        return words;
    }
}
