package com.desklampstudios.thyroxine;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncRequest;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;

import java.io.InputStream;
import java.util.Scanner;

public class Utils {
    public static String cleanHtml(String in) {
        return Html.fromHtml(in).toString().trim();
    }
    public static String getSnippet(String in, int length) {
        in = cleanHtml(in);
        in = in.replaceAll("\\s+", " ");
        if (length >= 0 && in.length() > length) {
            in = in.substring(0, length);
        }
        return in;
    }

    /**
     * Reads the full contents of an InputStream into a String.
     * Source: <a href="https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner.html">
     *     Stupid Scanner tricks</a>
     *
     * @param is The input stream to be read.
     * @return The contents of the stream, decoded with UTF-8.
     */
    public static String readInputStream(InputStream is) {
        return new Scanner(is, "UTF-8").useDelimiter("\\A").next();
    }

    /**
     * Helper method to schedule periodic execution of a sync adapter.
     * flexTime is only used on KitKat and newer devices.
     */
    public static void configurePeriodicSync(Account account, String authority,
                                             int syncInterval, int flexTime) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, syncInterval);
        }
    }
}
