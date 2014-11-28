package com.desklampstudios.thyroxine.news;

import android.text.Html;
import android.text.Spanned;

import com.desklampstudios.thyroxine.ListTagHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;

class IodineNewsEntry implements Serializable {
    private static final long serialVersionUID = 0L;

    public final String link;

    public String title;
    public Long published;
    public String contentRaw;

    // Parsed content fields: will not be saved when serialized
    public transient Spanned contentParsed;
    public transient String contentSnippet;

    public IodineNewsEntry(String link, String title, Long published, String content) {
        this.link = link.trim();
        this.title = title.trim();
        this.published = published;
        this.contentRaw = content;

        // Initialize parsed content fields
        this.contentParsed = parseContent(this.contentRaw);
        this.contentSnippet = getContentSnippet(this.contentParsed);
    }

    @Override
    public String toString() {
        return String.format("Title: %s\nPublished: %s\nLink: %s\nContent: %s\n",
                this.link, this.title, new Date(this.published), this.contentSnippet);
    }

    // Override default serialization handling
    private void readObject(ObjectInputStream inputStream)
            throws IOException, ClassNotFoundException {

        inputStream.defaultReadObject();

        // Reinitialize parsed content fields
        this.contentParsed = Html.fromHtml(this.contentRaw);
        this.contentSnippet = getContentSnippet(this.contentParsed);
    }


    // Helper methods to parse raw content
    private static Spanned parseContent(String contentRaw) {
        return Html.fromHtml(contentRaw, null, new ListTagHandler());
    }

    private static String getContentSnippet(Spanned contentParsed) {
        String s = contentParsed.toString();
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}