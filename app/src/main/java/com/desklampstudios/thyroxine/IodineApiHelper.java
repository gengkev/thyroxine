package com.desklampstudios.thyroxine;

import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

// Don't even THINK about touching this class if you're in the UI thread!
public class IodineApiHelper {
    private static final String TAG = IodineApiHelper.class.getSimpleName();
    private static final String IODINE_DOMAIN = "iodine.tjhsst.edu";
    private static final String IODINE_BASE_URL = "https://" + IODINE_DOMAIN;

    private static final String PUBLIC_NEWS_FEED_URL = IODINE_BASE_URL + "/feeds/rss";
    private static final String LOGIN_URL = IODINE_BASE_URL + "/api/";
    private static final String BLOCK_LIST_URL = IODINE_BASE_URL + "/api/eighth/list_blocks";
    private static final String BLOCK_GET_URL = IODINE_BASE_URL + "/api/eighth/get_block/%d";

    private static final URI IODINE_BASE_URI = URI.create("iodine.tjhsst.edu");
    private static final String SESSION_ID_COOKIE = "PHPSESSID";
    private static final String PASS_VECTOR_COOKIE = "IODINE_PASS_VECTOR";

    private static CookieManager getCookieManager() {
        CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
        if (cookieManager == null) {
            cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);
        }
        return cookieManager;
    }

    public static void clearCookies() {
        CookieManager cookieManager = getCookieManager();
        CookieStore cookieStore = cookieManager.getCookieStore();

        cookieStore.removeAll();

        Log.d(TAG, "Clearing cookies: " + cookieStore.get(IODINE_BASE_URI));
    }

    public static String getCookies() {
        CookieManager cookieManager = getCookieManager();
        CookieStore cookieStore = cookieManager.getCookieStore();

        String sessionId = "", passVector = "";
        for (HttpCookie cookie : cookieStore.get(IODINE_BASE_URI)) {
            if (cookie.getName().equals(SESSION_ID_COOKIE)) {
                sessionId = cookie.getValue();
            } else if (cookie.getName().equals(PASS_VECTOR_COOKIE)) {
                passVector = cookie.getValue();
            }
        }
        return new IodineCookieState(sessionId, passVector).stringify();
    }

    public static void setCookies(String in) {
        if (in.isEmpty()) return;
        CookieManager cookieManager = getCookieManager();
        CookieStore cookieStore = cookieManager.getCookieStore();
        IodineCookieState cookies = new IodineCookieState(in);

        HttpCookie sessionIdCookie = new HttpCookie(SESSION_ID_COOKIE, cookies.sessionId);
        HttpCookie passVectorCookie = new HttpCookie(PASS_VECTOR_COOKIE, cookies.passVector);
        sessionIdCookie.setDomain(IODINE_DOMAIN);
        passVectorCookie.setDomain(IODINE_DOMAIN);
        sessionIdCookie.setPath("/");
        passVectorCookie.setPath("/");
        sessionIdCookie.setVersion(0);
        passVectorCookie.setVersion(0);

        cookieStore.add(IODINE_BASE_URI, sessionIdCookie);
        cookieStore.add(IODINE_BASE_URI, passVectorCookie);

        Log.d(TAG, "Setting cookies: " + cookieStore.get(IODINE_BASE_URI));
    }

    public static InputStream getPrivateNewsFeed() throws IOException {
        return getPublicNewsFeed();
    }

    public static InputStream getPublicNewsFeed() throws IOException {
        URL url = new URL(PUBLIC_NEWS_FEED_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Response code invalid: " + conn.getResponseCode());
        }
        return new BufferedInputStream(conn.getInputStream());
    }

    public static InputStream getBlock(int bid) throws IOException {
        // log cookies
        CookieStore cookieStore = getCookieManager().getCookieStore();
        Log.d(TAG, "Cookies: " + cookieStore.get(IODINE_BASE_URI));

        URL url = new URL(String.format(BLOCK_GET_URL, bid));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Response code invalid: " + conn.getResponseCode());
        }
        return new BufferedInputStream(conn.getInputStream());
    }

    public static InputStream getBlockList() throws IOException {
        // log cookies
        CookieStore cookieStore = getCookieManager().getCookieStore();
        Log.d(TAG, "Cookies: " + cookieStore.get(IODINE_BASE_URI));

        URL url = new URL(BLOCK_LIST_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Response code invalid: " + conn.getResponseCode());
        }
        return new BufferedInputStream(conn.getInputStream());
    }

    public static void attemptLogin(String username, String password)
            throws IodineAuthException, IOException, XmlPullParserException {

        // create query params
        String query = "login_username=" + URLEncoder.encode(username, "UTF-8") +
                "&login_password=" + URLEncoder.encode(password, "UTF-8");

        // prepare cookies yum
        CookieManager cookieManager = getCookieManager();

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
                IodineAuthErrorParser parser = new IodineAuthErrorParser();
                parser.beginAuthError(is);
                authErr = parser.getError();
            } finally {
                is.close();
            }
            Log.w(TAG, "Login error: " + authErr.errCode, authErr);
            throw authErr;
        }

        // 302 Found: logged in successfully
        Log.i(TAG, "Login succeeded: status " + conn.getResponseCode());

        // cookies yum yum
        CookieStore cookieStore = cookieManager.getCookieStore();
        List<HttpCookie> cookies = cookieStore.get(IODINE_BASE_URI);
        Log.d(TAG, "Cookies: " + cookies);

        for (HttpCookie cookie : cookies) {
            if (cookie.getName().equals("PHPSESSID")) {
                return;
            }
        }

        // uh oh, wheres cookie :(
        Log.e(TAG, "Auth error: couldn't find cookie in response");
        Log.i(TAG, "Reading auth input stream: " + readInputStream(conn.getInputStream()));
        throw new IOException("Couldn't find cookie in response");
    }

    public static String readInputStream(InputStream is) throws IOException {
        // Stupid Scanner tricks
        // https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner.html
        return new Scanner(is, "UTF-8").useDelimiter("\\A").next();
    }

    public static class IodineCookieState {
        public final String sessionId;
        public final String passVector;

        public IodineCookieState(String sessionId, String passVector) {
            this.sessionId = sessionId;
            this.passVector = passVector;
        }

        public IodineCookieState(String in) {
            try {
                String[] split = in.split("\\|", 2); // fricking regexes
                this.sessionId = URLDecoder.decode(split[0], "UTF-8");
                this.passVector = URLDecoder.decode(split[1], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.wtf(TAG, "UTF-8 not supported", e);
                throw new RuntimeException(e);
            }
        }

        public String stringify() {
            try {
                String sessionIdEnc = URLEncoder.encode(this.sessionId, "UTF-8");
                String passVectorEnc = URLEncoder.encode(this.passVector, "UTF-8");
                return sessionIdEnc + "|" + passVectorEnc;
            } catch (UnsupportedEncodingException e) {
                Log.wtf(TAG, "UTF-8 not supported", e);
                throw new RuntimeException(e);
            }
        }
    }
}
