package com.desklampstudios.thyroxine.news;

import android.support.annotation.NonNull;

import java.util.Date;

class NewsEntry {
    @NonNull public String link;
    @NonNull public String title;
    public long published;
    @NonNull public String contentRaw;
    @NonNull public String contentSnippet;

    public NewsEntry(@NonNull String link, @NonNull String title, long published,
                     @NonNull String contentRaw, @NonNull String contentSnippet) {
        this.link = link;
        this.title = title;
        this.published = published;
        this.contentRaw = contentRaw;
        this.contentSnippet = contentSnippet;
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
}