package com.desklampstudios.thyroxine.eighth.io;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.desklampstudios.thyroxine.auth.IodineApiHelper;
import com.desklampstudios.thyroxine.auth.IodineAuthException;
import com.desklampstudios.thyroxine.eighth.model.EighthBlockAndActv;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class IodineEighthApi {
    private static final String TAG = IodineEighthApi.class.getSimpleName();

    @NonNull
    public static List<EighthBlockAndActv> fetchActivities(Context context, int blockId, String authToken)
            throws IodineAuthException, IOException, XmlPullParserException {
        Log.v(TAG, "Cookies: " + authToken);

        // Connect to server
        URL url = new URL(String.format(IodineApiHelper.BLOCK_GET_URL, blockId));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", authToken);

        IodineApiHelper.checkResponseCode(context, conn);

        // Parse data
        InputStream stream = new BufferedInputStream(conn.getInputStream());;
        EighthGetBlockParser parser = new EighthGetBlockParser(context);

        try {
            EighthBlockAndActv blockPair = parser.beginGetBlock(stream);
            Log.d(TAG, "Block: " + blockPair.block);
            return parser.parseActivities();
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
    public static List<EighthBlockAndActv> fetchSchedule(Context context, String authToken)
            throws IodineAuthException, IOException, XmlPullParserException {
        Log.v(TAG, "Cookies: " + authToken);

        // Connect to server
        URL url = new URL(IodineApiHelper.BLOCK_LIST_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", authToken);

        IodineApiHelper.checkResponseCode(context, conn);

        // Parse data
        InputStream stream = new BufferedInputStream(conn.getInputStream());;
        EighthListBlocksParser parser = new EighthListBlocksParser(context);

        try {
            parser.beginListBlocks(stream);
            return parser.parseBlocks();
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

    public static void doSignup(Context context, int blockId, int actvId, String authToken)
            throws EighthSignupException, IodineAuthException, IOException, XmlPullParserException {

        // create query params
        String query = "bid=" + URLEncoder.encode(String.valueOf(blockId), "UTF-8") +
                "&aid=" + URLEncoder.encode(String.valueOf(actvId), "UTF-8");

        // create request
        URL url = new URL(IodineApiHelper.SIGNUP_ACTIVITY_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Cookie", authToken);
        conn.setFixedLengthStreamingMode(query.length());
        conn.setInstanceFollowRedirects(false);

        // write output and begin request
        OutputStreamWriter outWriter = new OutputStreamWriter(conn.getOutputStream());
        outWriter.write(query);
        outWriter.close();

        IodineApiHelper.checkResponseCode(context, conn);

        // Parse data
        InputStream stream = new BufferedInputStream(conn.getInputStream());
        EighthSignupActvParser parser = new EighthSignupActvParser(context);

        try {
            parser.beginSignupActivity(stream);
            parser.checkResult();
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
}
