package com.desklampstudios.thyroxine.directory;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.AuthErrorParser;
import com.desklampstudios.thyroxine.IodineAuthException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class DirectoryInfoParser extends AbstractXMLParser {
    public static final String TAG = DirectoryInfoParser.class.getName();

    public DirectoryInfoParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginInfo(@NonNull InputStream in)
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
        }
        mParser.require(XmlPullParser.START_TAG, ns, "studentdirectory");
    }

    @NonNull
    public DirectoryInfo parseDirectoryInfo() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            throw new IllegalStateException();
        }

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "info");

        DirectoryInfo info = readDirectoryInfo(mParser);
        Log.d(TAG, "Directory info: " + info);

        // no more entries found
        stopParse();
        return info;
    }

    @NonNull
    public static DirectoryInfo readDirectoryInfo(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "info");

        DirectoryInfo.Builder builder = new DirectoryInfo.Builder();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "iodineuid":
                    builder.tjhsstId(readText(parser, "iodineuid"));
                    break;
                case "iodineuidnumber":
                    builder.iodineUid(readInt(parser, "iodineuidnumber"));
                    break;
                case "graduationyear":
                    builder.graduationYear(readInt(parser, "graduationyear"));
                    break;
                case "givenname":
                    builder.givenName(readText(parser, "givenname"));
                    break;
                case "middlename":
                    builder.middleName(readText(parser, "middlename"));
                    break;
                case "sn":
                    builder.surname(readText(parser, "sn"));
                    break;
                case "nickname":
                    builder.nickname(readText(parser, "nickname"));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return builder.build();
    }
}
