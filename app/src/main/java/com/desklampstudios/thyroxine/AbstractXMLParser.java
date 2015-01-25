package com.desklampstudios.thyroxine;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;

public abstract class AbstractXMLParser {
    private static final String TAG = AbstractXMLParser.class.getSimpleName();
    @Nullable protected static final String ns = null; // don't use namespaces

    protected final Context mContext;
    @NonNull protected final XmlPullParser mParser;
    @Nullable protected InputStream mInputStream;
    protected boolean parsingBegun = false;

    protected AbstractXMLParser(Context context) throws XmlPullParserException {
        mContext = context;
        mParser = Xml.newPullParser();
        mParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        mParser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
        mParser.setFeature(Xml.FEATURE_RELAXED, true);
    }

    public void stopParse() {
        parsingBegun = false;
        try {
            mParser.setInput(null);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "stopParse: error while trying to stop parse", e);
        }
        mInputStream = null;
    }

    @NonNull
    protected static String readText(@NonNull XmlPullParser parser, String tagName)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String str = parser.nextText();
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return str == null ? "" : str;
    }

    protected static int readInt(@NonNull XmlPullParser parser, String tagName)
            throws XmlPullParserException, IOException {
        String str = readText(parser, tagName);
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new XmlPullParserException("Invalid integer: " + str, parser, e);
        }
    }

    protected static boolean readBoolean(@NonNull XmlPullParser parser, String tagName)
            throws XmlPullParserException, IOException {
        String str = readText(parser, tagName);
        if ("true".equalsIgnoreCase(str)) {
            return true;
        } else if ("false".equalsIgnoreCase(str)) {
            return false;
        }
        try {
            return Integer.parseInt(str) != 0;
        } catch (NumberFormatException e) {
            throw new XmlPullParserException("Invalid boolean/integer: " + str, parser, e);
        }
    }

    protected static void skip(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
