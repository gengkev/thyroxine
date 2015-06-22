package com.desklampstudios.thyroxine.eighth.io;

import android.content.Context;

import com.desklampstudios.thyroxine.AbstractXMLParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

class EighthSignupActvParser extends AbstractXMLParser {
    public EighthSignupActvParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginSignupActivity(InputStream in)
            throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;

        mParser.nextTag();
        if (mParser.getName().equals("error")) { // Params error
            String message = readText(mParser, "error");
            throw new IOException(message);
        }
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "signup");
    }

    public void checkResult() throws XmlPullParserException, IOException, EighthSignupException {
        if (!parsingBegun) {
            throw new IllegalStateException();
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (mParser.getName()) {
                case "result": {
                    int result = readInt(mParser, "result");
                    if (result == 0) {
                        return;
                    } else {
                        throw EighthSignupException.create(result, mContext);
                    }
                }
                default:
                    skip(mParser);
                    break;
            }
        }

        // No result found
        throw new IllegalStateException();
    }
}
