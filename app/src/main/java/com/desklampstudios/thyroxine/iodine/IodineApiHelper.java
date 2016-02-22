package com.desklampstudios.thyroxine.iodine;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.desklampstudios.thyroxine.Utils;

import org.xmlpull.v1.XmlPullParserException;

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

    public static final String IODINE_DOMAIN = "iodine.tjhsst.edu";
    public static final String IODINE_BASE_URL = "https://" + IODINE_DOMAIN;
    public static final String LOGIN_URL = IODINE_BASE_URL + "/api/";

    public static final String PUBLIC_NEWS_FEED_URL = IODINE_BASE_URL + "/feeds/rss";
    public static final String NEWS_LIST_URL = IODINE_BASE_URL + "/api/news/list?end=100";
    public static final String NEWS_SHOW_URL = IODINE_BASE_URL + "/news/show/";

    public static final String BLOCK_LIST_URL = IODINE_BASE_URL + "/api/eighth/list_blocks";
    public static final String BLOCK_GET_URL = IODINE_BASE_URL + "/api/eighth/get_block/%d";
    public static final String SIGNUP_ACTIVITY_URL = IODINE_BASE_URL + "/api/eighth/signup_activity";

    public static final String DIRECTORY_INFO_URL = IODINE_BASE_URL + "/api/studentdirectory/info/%s";
    public static final String USER_ICON_URL = IODINE_BASE_URL + "/pictures/%s/main";

    private static final String SESSION_ID_COOKIE = "PHPSESSID";
    private static final String PASS_VECTOR_COOKIE = "IODINE_PASS_VECTOR";


    public static void checkResponseCode(Context context, HttpsURLConnection conn)
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

    @Nullable
    public static String attemptLogin(Context context, String username, String password)
            throws IOException, XmlPullParserException, IodineAuthException {

        // create query params
        String query = "login_username=" + URLEncoder.encode(username, "UTF-8") +
                "&login_password=" + URLEncoder.encode(password, "UTF-8");

        // create request
        URL url = new URL(IodineApiHelper.LOGIN_URL);
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
