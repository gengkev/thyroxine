package com.desklampstudios.thyroxine.news;

import java.util.Date;

class NewsEntry {
    public final String link;

    public String title;
    public long published;
    public String contentRaw;
    public String contentSnippet;

    public NewsEntry(String link, String title, long published,
                     String contentRaw, String contentSnippet) {
        assert link != null;
        assert title != null;
        assert contentRaw != null;

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