package com.desklampstudios.thyroxine.news;

import android.util.Log;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class IodineNewsFeedParser extends AbstractXMLParser {
    private static final String TAG = IodineNewsFeedParser.class.getSimpleName();
    private static final DateFormat FEED_DATE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    public IodineNewsFeedParser() throws XmlPullParserException {
        super();
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

    public NewsEntry nextEntry() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = mParser.getName();
            if (name.equals("item")) {
                return readEntry(mParser);
            } else {
                skip(mParser);
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    // Parses the contents of an entry. If it encounters a title, published, link, or content tag,
    // those are handed off to their respective "read" methods. Other tags are ignored.
    private static NewsEntry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "item");

        String title = null;
        long published = 0;
        String link = null;
        String content = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = Utils.cleanHtml(readText(parser, "title"));
            } else if (name.equals("pubDate")) {
                published = readPublished(parser);
            } else if (name.equals("link")) {
                link = Utils.cleanHtml(readText(parser, "link"));
            } else if (name.equals("description")) {
                content = readText(parser, "description");
            } else {
                skip(parser);
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "item");

        if (title == null || published == 0 || link == null || content == null) {
            Log.w(TAG, String.format("readEntry: title (%s) or published (%s) or link (%s) " +
                    "or content (%s) not found", title, published, link, content));

            title = (title == null) ? "" : title;
            link = (link == null) ? "" : link;
            content = (content == null) ? "" : content;
        }

        String snippet = Utils.getSnippet(content, 300);

        return new NewsEntry(link, title, published, content, snippet);
    }

    // Process published tags in the feed.
    private static Long readPublished(XmlPullParser parser) throws IOException, XmlPullParserException {
        String publishedStr = readText(parser, "pubDate");
        Long published = null;
        try {
            Date date = FEED_DATE_FORMAT.parse(publishedStr);
            published = date.getTime();
        } catch (ParseException e) {
            Log.e(TAG, "datetime parse exception: " + publishedStr + ", " + e.toString());
        }
        return published;
    }
}

