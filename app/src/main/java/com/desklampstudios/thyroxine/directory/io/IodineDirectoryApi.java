package com.desklampstudios.thyroxine.directory.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Log;

import com.desklampstudios.thyroxine.auth.IodineApiHelper;
import com.desklampstudios.thyroxine.auth.IodineAuthException;
import com.desklampstudios.thyroxine.directory.model.DirectoryInfo;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class IodineDirectoryApi {
    private static final String TAG = IodineDirectoryApi.class.getSimpleName();

    @NonNull
    public static DirectoryInfo getDirectoryInfo(Context context, String uid, String authToken)
            throws XmlPullParserException, IOException, IodineAuthException {
        Log.v(TAG, "Cookies: " + authToken);

        // Connect to server
        URL url = new URL(String.format(IodineApiHelper.DIRECTORY_INFO_URL, uid));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", authToken);

        IodineApiHelper.checkResponseCode(context, conn);

        // Parse data
        InputStream stream = new BufferedInputStream(conn.getInputStream());
        DirectoryInfoParser parser = new DirectoryInfoParser(context);

        try {
            parser.beginInfo(stream);
            return parser.parseDirectoryInfo();
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
    public static Bitmap getUserIcon(Context context, String uid, String authToken)
            throws XmlPullParserException, IOException, IodineAuthException {
        Log.v(TAG, "Cookies: " + authToken);

        // Connect to server
        URL url = new URL(String.format(IodineApiHelper.USER_ICON_URL, uid));
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", authToken);

        IodineApiHelper.checkResponseCode(context, conn);

        // Parse data
        InputStream stream = new BufferedInputStream(conn.getInputStream());

        try {
            return BitmapFactory.decodeStream(stream);
        }
        finally {
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException when closing stream", e);
            }
        }
    }
}
