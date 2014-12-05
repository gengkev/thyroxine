package com.desklampstudios.thyroxine;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractXMLParser {
    private static final String TAG = AbstractXMLParser.class.getSimpleName();
    protected static final String ns = null; // don't use namespaces

    protected final XmlPullParser mParser;
    protected InputStream mInputStream;
    protected boolean parsingBegun = false;

    protected AbstractXMLParser() throws XmlPullParserException {
        mParser = Xml.newPullParser();
        mParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        mParser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
        mParser.setFeature(Xml.FEATURE_RELAXED, true);
    }

    protected void stopParse() throws XmlPullParserException {
        parsingBegun = false;
        mParser.setInput(null);
        mInputStream = null;
    }

    protected static String readText(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String str = parser.nextText();
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return str;
    }

    protected static Integer readInt(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        String str = readText(parser, tagName);
        Integer integer = null;
        try {
            integer = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Log.e(TAG, "readInt parse exception for int: " + str + ", " + e.toString());
        }
        return integer;
    }

    protected static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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
