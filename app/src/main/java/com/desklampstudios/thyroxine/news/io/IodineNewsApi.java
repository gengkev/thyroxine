package com.desklampstudios.thyroxine.news.io;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.desklampstudios.thyroxine.iodine.IodineApiHelper;
import com.desklampstudios.thyroxine.iodine.IodineAuthException;
import com.desklampstudios.thyroxine.news.model.NewsEntry;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class IodineNewsApi {
    private static final String TAG = IodineNewsApi.class.getSimpleName();

    @NonNull
    public static List<NewsEntry> fetchPublicNewsFeed(Context context)
            throws IodineAuthException, IOException, XmlPullParserException {
        // Connect to server
        URL url = new URL(IodineApiHelper.PUBLIC_NEWS_FEED_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        IodineApiHelper.checkResponseCode(context, conn);

        // Parse data
        InputStream stream = new BufferedInputStream(conn.getInputStream());
        NewsFeedParser parser = new NewsFeedParser(context);

        try {
            parser.beginFeed(stream);
            return parser.parseEntries();
        }
        finally {
            parser.stopParse();
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream", e);
            }
        }
    }


    @NonNull
    public static List<NewsEntry> fetchNewsList(Context context, String authToken)
            throws IodineAuthException, IOException, XmlPullParserException {
        Log.v(TAG, "Cookies: " + authToken);

        // Connect to server
        URL url = new URL(IodineApiHelper.NEWS_LIST_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", authToken);

        IodineApiHelper.checkResponseCode(context, conn);

        // Parse data
        InputStream stream = new BufferedInputStream(conn.getInputStream());;
        NewsListParser parser = new NewsListParser(context);

        try {
            parser.beginFeed(stream);
            return parser.parseEntries();
        }
        finally {
            parser.stopParse();
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream", e);
            }
        }
    }

    @NonNull
    public static String getNewsShowUrl(int newsId) {
        return IodineApiHelper.NEWS_SHOW_URL + newsId;
    }
}
