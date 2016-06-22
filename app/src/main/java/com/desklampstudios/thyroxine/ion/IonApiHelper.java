package com.desklampstudios.thyroxine.ion;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class IonApiHelper {
    public static final String AUTHORIZATION_ENDPOINT = "https://ion.tjhsst.edu/oauth/authorize/";
    public static final String TOKEN_ENDPOINT = "https://ion.tjhsst.edu/oauth/token/";
    public static final String REDIRECT_URL = "http://localhost/oauth_redirect";

    static final String PROFILE_BASE_URL = "https://ion.tjhsst.edu/api/profile/";

    private static final String CLIENT_ID = "n0UIi7wEifvoqyUy0FkCWYF72BpwIBrqalQ8qd6P";
    private static final String CLIENT_SECRET = "pObxsOaNEthE4cz94D7NJWIATNL7yGd2FW3jxLFp6cdUW5N" +
            "0FOeYbrJ5Vrw7HTJ7kcdoRMkEUkW8fyN7sGUzWaOYr0LUWSjqn0oKQ2qv8XmBdVuyqLKtjXDnMpugXNEw";
    private static final String SCOPE = "read write";
    private static final String RESPONSE_TYPE = "code";
    private static final String GRANT_TYPE = "authorization_code";

    public static String makeAuthURL(String state) {
        return Uri.parse(AUTHORIZATION_ENDPOINT).buildUpon()
                .appendQueryParameter("response_type", RESPONSE_TYPE)
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_url", REDIRECT_URL)
                .appendQueryParameter("scope", SCOPE)
                .appendQueryParameter("state", state)
                .build().toString();
    }

    public static Uri makeProfileImageURL(String id) {
        return Uri.parse(PROFILE_BASE_URL).buildUpon()
                .appendPath(id)
                .appendPath("picture")
                .build();
    }

    static String getAuthToken(String code) throws IOException {
        String query = Uri.EMPTY.buildUpon()
                .appendQueryParameter("code", code)
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("client_secret", CLIENT_SECRET)
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .appendQueryParameter("grant_type", GRANT_TYPE)
                .build().getQuery();

        // create request
        URL url = new URL(TOKEN_ENDPOINT);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setFixedLengthStreamingMode(query.length());

        // write output and begin request
        OutputStreamWriter outWriter = new OutputStreamWriter(conn.getOutputStream());
        outWriter.write(query);
        outWriter.close();

        if (conn.getResponseCode() != 200) {
            throw new IOException("Bad response code: " + conn.getResponseCode());
        }

        InputStream is = conn.getInputStream();
        String result = null;
        try {
            Scanner sc = new java.util.Scanner(is).useDelimiter("\\A");
            result = sc.next();
        } finally {
            is.close();
        }

        return result;
    }

    static void putToken(HttpsURLConnection conn, String authToken) {
        conn.setRequestProperty("Authorization", "Bearer " + authToken);
    }
}
