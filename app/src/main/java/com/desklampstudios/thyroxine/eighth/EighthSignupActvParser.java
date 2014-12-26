package com.desklampstudios.thyroxine.eighth;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.AuthErrorParser;
import com.desklampstudios.thyroxine.IodineAuthException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class EighthSignupActvParser extends AbstractXMLParser {
    public EighthSignupActvParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginSignupActivity(InputStream in)
            throws XmlPullParserException, IOException, IodineAuthException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;

        mParser.nextTag();
        if (mParser.getName().equals("auth")) { // Auth error
            throw AuthErrorParser.readAuth(mParser, mContext);
        } else if (mParser.getName().equals("error")) { // Params error
            String message = readText(mParser, "error");
            throw new IOException(message);
        }
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "signup");
    }

    @Nullable
    public Integer nextResult() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (mParser.getName()) {
                case "result":
                    return readInt(mParser, "result");
                default:
                    skip(mParser);
                    break;
            }
        }

        // No more entries found
        stopParse();
        return null;
    }
}
