package com.desklampstudios.thyroxine;

import android.text.Html;

import java.io.IOException;
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
    public static String readInputStream(InputStream is) {
        // Stupid Scanner tricks
        // https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner.html
        return new Scanner(is, "UTF-8").useDelimiter("\\A").next();
    }
}
