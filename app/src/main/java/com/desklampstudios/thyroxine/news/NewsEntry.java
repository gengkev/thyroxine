package com.desklampstudios.thyroxine.news;

import android.support.annotation.NonNull;

import java.util.Date;

class NewsEntry {
    public final int newsId;
    @NonNull public final String title;
    public final long published;
    @NonNull public final String content;
    @NonNull public final String contentSnippet;
    public final boolean liked; // warning: user-specific
    public final int numLikes;

    public NewsEntry(Builder builder) {
        this.newsId = builder.newsId;
        this.title = builder.title;
        this.published = builder.published;
        this.content = builder.content;
        this.contentSnippet = builder.contentSnippet;
        this.liked = builder.liked;
        this.numLikes = builder.numLikes;
    }

    @Override
    public String toString() {
        return String.format("NewsEntry[newsId=%d, title=%s, published=%s, content=%s, " +
                        "liked=%b, numLikes=%d]",
                newsId, title, new Date(published), contentSnippet, liked, numLikes);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NewsEntry)) return false;
        NewsEntry entry = (NewsEntry) o;
        return newsId == entry.newsId && title.equals(entry.title) &&
                published == entry.published && content.equals(entry.content) &&
                liked == entry.liked && numLikes == entry.numLikes;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public static class Builder {
        private int newsId;
        @NonNull private String title;
        private long published;
        @NonNull private String content;
        @NonNull private String contentSnippet;
        private boolean liked;
        private int numLikes;

        public Builder() {}

        public Builder newsId(int newsId) {
            this.newsId = newsId;
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
        public Builder content(@NonNull String content) {
            this.content = content;
            return this;
        }
        public Builder contentSnippet(@NonNull String contentSnippet) {
            this.contentSnippet = contentSnippet;
            return this;
        }
        public Builder liked(boolean liked) {
            this.liked = liked;
            return this;
        }
        public Builder numLikes(int numLikes) {
            this.numLikes = numLikes;
            return this;
        }

        public NewsEntry build() {
            NewsEntry entry = new NewsEntry(this);
            return entry;
        }
    }
}