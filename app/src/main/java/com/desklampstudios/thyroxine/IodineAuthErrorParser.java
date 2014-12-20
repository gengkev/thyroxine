package com.desklampstudios.thyroxine;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

class IodineAuthErrorParser extends AbstractXMLParser {
    private static final String TAG = IodineAuthErrorParser.class.getSimpleName();

    public IodineAuthErrorParser() throws XmlPullParserException {
        super();
    }

    public void beginAuthError(InputStream in) throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "auth");

        parsingBegun = true;
    }

    public IodineAuthException getError() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = mParser.getName();
            if (name.equals("error")) {
                return readError(mParser);
            } else {
                skip(mParser);
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    private static IodineAuthException readError(XmlPullParser parser) throws XmlPullParserException, IOException {
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

        return new IodineAuthException(id);
    }
}
