package com.desklampstudios.thyroxine;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class AuthErrorParser extends AbstractXMLParser {
    private static final String TAG = AuthErrorParser.class.getSimpleName();

    public AuthErrorParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginAuthError(InputStream in) throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;
    }

    public IodineAuthException nextAuth() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (mParser.getName()) {
                case "auth":
                    return readAuth(mParser, mContext);
                default:
                    skip(mParser);
                    break;
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    public static IodineAuthException readAuth(XmlPullParser parser, Context context)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "auth");

        IodineAuthException ex = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "error":
                    ex = readError(parser, context);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "auth");
        return ex;
    }

    private static IodineAuthException readError(XmlPullParser parser, Context context)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "error");

        Integer id = null;
        String message = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "id":
                    id = readInt(parser, "id");
                    break;
                case "message":
                    message = readText(parser, "message");
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "error");

        return IodineAuthException.create(id, message, context);
    }
}