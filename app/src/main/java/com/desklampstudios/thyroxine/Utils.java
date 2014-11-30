package com.desklampstudios.thyroxine;

import android.text.Spanned;

public class Utils {
    public static String getSnippet(Spanned parsed, int length) {
        String s = parsed.toString();
        s = s.replaceAll("\\s+", " ").trim();
        if (length >= 0 && s.length() > length) {
            s = s.substring(0, length);
        }
        return s;
    }
}
