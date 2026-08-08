package org.example.civitaswebapp.domain;

public enum MemberLanguage {
    NL,
    TR,
    EN;

    /** Language assumed when a member has none on file. Civitas targets Flanders. */
    public static final MemberLanguage DEFAULT = NL;

    /**
     * Null-safe accessor. Rows written before this column existed can still hold NULL, and the
     * WhatsApp send path must never fail on that — callers log the fallback where they have the
     * context to make it actionable.
     */
    public static MemberLanguage orDefault(MemberLanguage language) {
        return language != null ? language : DEFAULT;
    }
}
