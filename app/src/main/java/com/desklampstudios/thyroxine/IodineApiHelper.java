package com.desklampstudios.thyroxine;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class IodineApiHelper {
    private static final String TAG = IodineApiHelper.class.getSimpleName();
    private static final String IODINE_DOMAIN = "iodine.tjhsst.edu";
    private static final String IODINE_BASE_URL = "https://" + IODINE_DOMAIN;
    private static final String LOGIN_URL = IODINE_BASE_URL + "/api/";

    private static final String PUBLIC_NEWS_FEED_URL = IODINE_BASE_URL + "/feeds/rss";
    private static final String NEWS_LIST_URL = IODINE_BASE_URL + "/api/news/list?end=100";
    public static final String NEWS_SHOW_URL = IODINE_BASE_URL + "/news/show/";

    private static final String BLOCK_LIST_URL = IODINE_BASE_URL + "/api/eighth/list_blocks";
    private static final String BLOCK_GET_URL = IODINE_BASE_URL + "/api/eighth/get_block/%d";
    private static final String SIGNUP_ACTIVITY_URL = IODINE_BASE_URL + "/api/eighth/signup_activity";

    private static final String DIRECTORY_INFO_URL = IODINE_BASE_URL + "/api/studentdirectory/info/%s";
    private static final String USER_ICON_URL = IODINE_BASE_URL + "/pictures/%s/main";

    private static final String SESSION_ID_COOKIE = "PHPSESSID";
    private static final String PASS_VECTOR_COOKIE = "IODINE_PASS_VECTOR";

    private static void checkResponseCode(Context context, HttpsURLConnection conn)
            throws IOException, XmlPullParserException, IodineAuthException {

        // Note that to read 4xx response codes, one must use getErrorStream() (see #16)
        switch (conn.getResponseCode()) {
            case 200:
                break;
            case 401:
                AuthErrorParser parser = new AuthErrorParser(context);
                parser.beginAuthError(conn.getErrorStream());
                throw parser.nextAuth();
            default:
                throw new IOException("Unexpected response code: " + conn.getResponseCode());
        }
    }

    @NonNull
    public static InputStream getPublicNewsFeed(Context context)
            throws IOException, XmlPullParserException, IodineAuthException {
        URL url = new URL(PUBLIC_NEWS_FEED_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        checkResponseCode(context, conn);
        return new BufferedInputStream(conn.getInputStream());
    }

    @NonNull
    public static InputStream getNewsList(Context context, String cookieHeader)
            throws IOException, XmlPullParserException, IodineAuthException {
        Log.v(TAG, "Cookies: " + cookieHeader);

        URL url = new URL(NEWS_LIST_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", cookieHeader);

        checkResponseCode(context, conn);
        return new BufferedInputStream(conn.getInputStream());
    }

    @NonNull
    public static String getNewsShowUrl(int newsId) {
        return NEWS_SHOW_URL + newsId;
    }

    @NonNull
    public static InputStream getBlock(Context context, int blockId, String cookieHeader)
            throws IOException, XmlPullParserException, IodineAuthException {
        Log.v(TAG, "Cookies: " + cookieHeader);

        URL url = new URL(String.format(BLOCK_GET_URL, blockId));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", cookieHeader);

        checkResponseCode(context, conn);
        return new BufferedInputStream(conn.getInputStream());
    }

    @NonNull
    public static InputStream getBlockList(Context context, String cookieHeader)
            throws IOException, XmlPullParserException, IodineAuthException {
        Log.v(TAG, "Cookies: " + cookieHeader);

        URL url = new URL(BLOCK_LIST_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", cookieHeader);

        checkResponseCode(context, conn);
        return new BufferedInputStream(conn.getInputStream());
    }

    @NonNull
    public static InputStream signupActivity(Context context, int blockId, int actvId, String cookieHeader)
            throws IOException, XmlPullParserException, IodineAuthException {

        // create query params
        String query = "bid=" + URLEncoder.encode(String.valueOf(blockId), "UTF-8") +
                "&aid=" + URLEncoder.encode(String.valueOf(actvId), "UTF-8");

        // create request
        URL url = new URL(SIGNUP_ACTIVITY_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Cookie", cookieHeader);
        conn.setFixedLengthStreamingMode(query.length());
        conn.setInstanceFollowRedirects(false);

        // write output and begin request
        OutputStreamWriter outWriter = new OutputStreamWriter(conn.getOutputStream());
        outWriter.write(query);
        outWriter.close();

        checkResponseCode(context, conn);
        return new BufferedInputStream(conn.getInputStream());
    }

    @NonNull
    public static InputStream getDirectoryInfo(Context context, String uid, String cookieHeader)
            throws IOException, XmlPullParserException, IodineAuthException {
        Log.v(TAG, "Cookies: " + cookieHeader);

        URL url = new URL(String.format(DIRECTORY_INFO_URL, uid));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", cookieHeader);

        checkResponseCode(context, conn);
        return new BufferedInputStream(conn.getInputStream());
    }

    @NonNull
    public static InputStream getUserIcon(Context context, String uid, String cookieHeader)
            throws IOException, XmlPullParserException, IodineAuthException {
        Log.v(TAG, "Cookies: " + cookieHeader);

        URL url = new URL(String.format(USER_ICON_URL, uid));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", cookieHeader);

        checkResponseCode(context, conn);
        return new BufferedInputStream(conn.getInputStream());
    }

    @Nullable
    public static String attemptLogin(Context context, String username, String password)
            throws IOException, XmlPullParserException, IodineAuthException {

        // create query params
        String query = "login_username=" + URLEncoder.encode(username, "UTF-8") +
                "&login_password=" + URLEncoder.encode(password, "UTF-8");

        // create request
        URL url = new URL(LOGIN_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(query.length());
        conn.setInstanceFollowRedirects(false);

        // write output and begin request
        OutputStreamWriter outWriter = new OutputStreamWriter(conn.getOutputStream());
        outWriter.write(query);
        outWriter.close();

        if (conn.getResponseCode() != 302) {
            // status code not 302 Found: login failed :(
            Log.w(TAG, "Login failed: status " + conn.getResponseCode());

            IodineAuthException authErr = null;
            InputStream is = conn.getInputStream();
            try {
                AuthErrorParser parser = new AuthErrorParser(context);
                parser.beginAuthError(is);
                authErr = parser.nextAuth();
            } finally {
                is.close();
            }
            Log.w(TAG, "Iodine auth error occurred", authErr);
            throw authErr;
        }

        // 302 Found: logged in successfully
        Log.i(TAG, "Login succeeded: status " + conn.getResponseCode());

        // cookies yum yum
        List<HttpCookie> cookies = new ArrayList<>();
        List<String> cookieHeaders = conn.getHeaderFields().get("Set-Cookie");
        for (String header : cookieHeaders) {
            cookies.addAll(HttpCookie.parse(header));
        }

        Log.v(TAG, "Cookie headers: " + cookieHeaders);
        Log.v(TAG, "Cookies: " + cookies);

        HttpCookie sessionId = null, passVector = null;

        for (HttpCookie cookie : cookies) {
            if (!cookie.hasExpired()) {
                if (cookie.getName().equals(SESSION_ID_COOKIE)) {
                    sessionId = cookie;
                } else if (cookie.getName().equals(PASS_VECTOR_COOKIE)) {
                    passVector = cookie;
                }
            }
        }

        // uh oh, wheres cookie :(
        if (sessionId == null || passVector == null) {
            Log.e(TAG, "Auth error: couldn't find cookie in response");
            Log.v(TAG, "Reading auth input stream: " + Utils.readInputStream(conn.getInputStream()));
            throw new IOException("Couldn't find cookie in response");
        }

        String cookieString = sessionId + "; " + passVector;
        Log.v(TAG, "Generated cookie string: " + cookieString);
        return cookieString;
    }
}
