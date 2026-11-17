package com.nth.desktop3d;

import java.text.Normalizer;
import java.util.Locale;

/** Routing rules never manufacture current facts or execute launcher actions. */
final class LiveQuestionPolicy {
    static String words(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }
    static boolean nextGame(String question) {
        String q = words(question);
        return q.matches(".*\\b(next|upcoming|again)\\b.*")
            && q.matches(".*\\b(play|plays|playing|game|games|match|opponent|schedule)\\b.*");
    }
    static boolean creative(String question) {
        String q = words(question);
        return !current(question) && q.matches("^(write|draft|compose|rewrite|proofread|brainstorm|create|give me ideas|help me write|help me plan)\\b.*");
    }
    static boolean current(String question) {
        return words(question).matches(".*\\b(now|today|tonight|tomorrow|yesterday|current|currently|latest|recent|next|upcoming|live|score|scores|weather|forecast|price|prices|news|president|ceo|mayor|governor|leader|prime minister)\\b.*");
    }
    static boolean needsSpecialSource(String question) {
        return words(question).matches(".*\\b(score|scores|weather|forecast|price|prices|news|headlines|schedule|opponent|playing)\\b.*") || nextGame(question);
    }
    static boolean settings(String question, String context) {
        String q = words(question);
        if (q.matches(".*\\b(launcher|nth desktop|setting|settings|tilt|parallax|dock|scene lighting|icon shape|folder shape|home screen|desktop transition)\\b.*")) return true;
        for (String line : context.split("\n")) {
            int colon = line.indexOf(':');
            if (colon > 0) { String title = words(line.substring(0, colon)); if (title.length() > 6 && q.contains(title)) return true; }
        }
        return false;
    }
    static String searchTerms(String question) {
        String q = question.trim().replaceAll("(?i)^(who|what|where|when|why|how)\\s+(is|are|was|were|does|do|did)\\s+", "")
            .replaceAll("(?i)\\b(the|a|an|please|currently|right now|today)\\b", " ").replaceAll("[?!]+$", "").trim().replaceAll("\\s+", " ");
        return q.isEmpty() ? question.trim() : q;
    }
    static String limit(String text, int max) { return text.length() <= max ? text : text.substring(0, max); }
}
