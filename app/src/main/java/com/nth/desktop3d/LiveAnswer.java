package com.nth.desktop3d;

import java.net.URI;
import java.util.*;

/** Source links are supplied by retrieval, never by generated answer text. */
final class LiveAnswer {
    static final class Source {
        final String title, url, text, detail;
        Source(String title, String url, String text, String detail) {
            if (!safeLink(url)) throw new IllegalArgumentException("Invalid source link");
            this.title = title; this.url = url; this.text = text; this.detail = detail;
        }
    }
    final String text, kind;
    final long checkedAt;
    final List<Source> sources;
    final boolean searchFallback;
    LiveAnswer(String text, String kind, long checkedAt, List<Source> sources, boolean searchFallback) {
        this.text = text; this.kind = kind; this.checkedAt = checkedAt;
        this.sources = Collections.unmodifiableList(new ArrayList<>(sources)); this.searchFallback = searchFallback;
    }
    static boolean safeLink(String url) {
        try { URI uri = new URI(url); return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
            && uri.getRawUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443); }
        catch (Exception invalid) { return false; }
    }
    static LiveAnswer plain(String text, String kind) { return new LiveAnswer(text, kind, 0, Collections.emptyList(), false); }
    static LiveAnswer unavailable(String text, long now, List<Source> sources) { return new LiveAnswer(text, "Could not verify", now, sources, true); }
}
