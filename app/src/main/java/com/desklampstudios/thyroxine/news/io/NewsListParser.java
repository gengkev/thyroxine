package com.desklampstudios.thyroxine.news.io;

import android.content.Context;
import android.support.annotation.NonNull;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.news.model.NewsEntry;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

class NewsListParser extends AbstractXMLParser {
    private static final String TAG = NewsListParser.class.getSimpleName();

    public NewsListParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginFeed(InputStream in) throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "news");

        parsingBegun = true;
    }

    @NonNull
    public ArrayList<NewsEntry> parseEntries() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            throw new IllegalStateException();
        }
        ArrayList<NewsEntry> entries = new ArrayList<>();

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (mParser.getName()) {
                case "post":
                    entries.add(readEntry(mParser));
                    break;
                default:
                    skip(mParser);
                    break;
            }
        }

        // No more entries found
        stopParse();
        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, published, link, or content tag,
    // those are handed off to their respective "read" methods. Other tags are ignored.
    @NonNull
    private static NewsEntry readEntry(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "post");

        NewsEntry.Builder newsBuilder = new NewsEntry.Builder();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "posted":
                    newsBuilder.published(
                            readPublished(parser));
                    break;
                case "id":
                    newsBuilder.newsId(
                            readInt(parser, "id"));
                    break;
                case "title":
                    newsBuilder.title(
                            Utils.cleanHtml(readText(parser, "title")));
                    break;
                case "text": {
                    String content = readText(parser, "text");
                    String snippet = Utils.getSnippet(content, 300);
                    newsBuilder.content(content);
                    newsBuilder.contentSnippet(snippet);
                    break;
                }
                case "liked":
                    newsBuilder.liked(
                            readBoolean(parser, "liked"));
                    break;
                case "likecount":
                    newsBuilder.numLikes(
                            readInt(parser, "likecount"));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "post");

        return newsBuilder.build();
    }

    // Process published tags in the feed.
    private static long readPublished(XmlPullParser parser) throws IOException, XmlPullParserException {
        String publishedStr = readText(parser, "posted");
        long published;
        try {
            Date date = Utils.FixedDateFormats.NEWS_LIST.parse(publishedStr);
            published = date.getTime();
        } catch (ParseException e) {
            throw new XmlPullParserException("Invalid date string: " + publishedStr, parser, e);
        }
        return published;
    }
}

