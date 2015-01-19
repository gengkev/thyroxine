package com.desklampstudios.thyroxine.news;

import android.support.annotation.NonNull;

import java.util.Date;

class NewsEntry {
    @NonNull public final String link;
    @NonNull public final String title;
    public final long published;
    @NonNull public final String contentRaw;
    @NonNull public final String contentSnippet;

    public NewsEntry(Builder builder) {
        this.link = builder.link;
        this.title = builder.title;
        this.published = builder.published;
        this.contentRaw = builder.contentRaw;
        this.contentSnippet = builder.contentSnippet;
    }

    @Override
    public String toString() {
        return String.format("NewsEntry[link=%s, title=%s, published=%s, content=%s]",
                this.link, this.title, new Date(this.published), this.contentSnippet);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NewsEntry)) return false;
        NewsEntry entry = (NewsEntry) o;
        return link.equals(entry.link) && title.equals(entry.title) &&
                published == entry.published && contentRaw.equals(entry.contentRaw);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public static class Builder {
        @NonNull private String link;
        @NonNull private String title;
        private long published;
        @NonNull private String contentRaw;
        @NonNull private String contentSnippet;

        public Builder() {}

        public Builder link(@NonNull String link) {
            this.link = link;
            return this;
        }
        public Builder title(@NonNull String title) {
            this.title = title;
            return this;
        }
        public Builder published(long published) {
            this.published = published;
            return this;
        }
        public Builder contentRaw(@NonNull String contentRaw) {
            this.contentRaw = contentRaw;
            return this;
        }
        public Builder contentSnippet(@NonNull String contentSnippet) {
            this.contentSnippet = contentSnippet;
            return this;
        }

        public NewsEntry build() {
            NewsEntry entry = new NewsEntry(this);
            return entry;
        }
    }
}