package com.desklampstudios.thyroxine;

import android.content.Context;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class IodineAuthErrorParser extends AbstractXMLParser {
    private static final String TAG = IodineAuthErrorParser.class.getSimpleName();

    public IodineAuthErrorParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginAuthError(InputStream in) throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "auth");
    }

    public IodineAuthException getError() throws XmlPullParserException, IOException {
        return getErrorStatic(this);
    }

    public static IodineAuthException getErrorStatic(AbstractXMLParser xmlParser)
            throws XmlPullParserException, IOException {
        if (!xmlParser.parsingBegun) {
            return null;
        }

        while (xmlParser.mParser.next() != XmlPullParser.END_TAG) {
            if (xmlParser.mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = xmlParser.mParser.getName();
            if (name.equals("error")) {
                return readError(xmlParser.mParser, xmlParser.mContext);
            } else {
                skip(xmlParser.mParser);
            }
        }

        // No more entries found
        xmlParser.stopParse();
        return null;
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
            String name = parser.getName();
            switch (name) {
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

        IodineAuthException e = IodineAuthException.create(id, message, context);
        Log.d(TAG, "Parsed auth error", e);
        return e;
    }
}