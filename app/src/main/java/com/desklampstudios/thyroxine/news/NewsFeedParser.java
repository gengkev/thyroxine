package com.desklampstudios.thyroxine.news;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;

class NewsFeedParser extends AbstractXMLParser {
    private static final String TAG = NewsFeedParser.class.getSimpleName();

    public NewsFeedParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginFeed(InputStream in) throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "rss");
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "channel");

        parsingBegun = true;
    }

    @Nullable
    public NewsEntry nextEntry() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (mParser.getName()) {
                case "item":
                    return readEntry(mParser);
                default:
                    skip(mParser);
                    break;
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    // Parses the contents of an entry. If it encounters a title, published, link, or content tag,
    // those are handed off to their respective "read" methods. Other tags are ignored.
    @NonNull
    private static NewsEntry readEntry(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");

        NewsEntry.Builder newsBuilder = new NewsEntry.Builder();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "title":
                    newsBuilder.title(Utils.cleanHtml(readText(parser, "title")));
                    break;
                case "pubDate":
                    newsBuilder.published(readPublished(parser));
                    break;
                case "link":
                    newsBuilder.link(Utils.cleanHtml(readText(parser, "link")));
                    break;
                case "description": {
                    String content = readText(parser, "description");
                    String snippet = Utils.getSnippet(content, 300);
                    newsBuilder.contentRaw(content);
                    newsBuilder.contentSnippet(snippet);
                    break;
                }
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "item");

        return newsBuilder.build();
    }

    // Process published tags in the feed.
    private static long readPublished(XmlPullParser parser) throws IOException, XmlPullParserException {
        String publishedStr = readText(parser, "pubDate");
        long published = 0;
        try {
            Date date = Utils.FixedDateFormats.NEWS_FEED.parse(publishedStr);
            published = date.getTime();
        } catch (ParseException e) {
            Log.e(TAG, "datetime parse exception: " + publishedStr + ", " + e.toString());
        }
        return published;
    }
}

