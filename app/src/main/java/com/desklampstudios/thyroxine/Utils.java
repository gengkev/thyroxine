package com.desklampstudios.thyroxine;

import android.text.Html;

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
}
