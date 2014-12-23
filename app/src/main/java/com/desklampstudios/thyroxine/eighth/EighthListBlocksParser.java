package com.desklampstudios.thyroxine.eighth;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.AuthErrorParser;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

// TODO: split into a parser for listBlocks and getBlock
class EighthListBlocksParser extends AbstractXMLParser {
    private static final String TAG = EighthListBlocksParser.class.getSimpleName();

    public EighthListBlocksParser(Context context) throws XmlPullParserException {
        super(context);
    }

    // after this, call nextBlock until it returns null
    public void beginListBlocks(InputStream in) throws XmlPullParserException, IOException, IodineAuthException {
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
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");


        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "blocks");
    }

    // Use with beginListBlocks
    @Nullable
    public Pair<EighthBlock, Integer> nextBlock() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = mParser.getName();
            switch (name) {
                case "block":
                    return readBlock(mParser);
                default:
                    skip(mParser);
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    @NonNull
    static Pair<EighthBlock, Integer> readBlock(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "block");

        int blockId = -1;
        String date = "";
        String type = "";
        boolean locked = false;

        Pair<EighthActv, EighthActvInstance> actvPair = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "bid":
                    blockId = readInt(parser, "bid");
                    break;
                case "date":
                    date = readBasicDate(parser);
                    break;
                case "type":
                    type = readText(parser, "type");
                    break;
                case "block":
                    type = readText(parser, "block");
                    break;
                case "activity":
                    actvPair = EighthGetBlockParser.readActivity(parser);
                    break;
                case "locked":
                    locked = readInt(parser, "locked") != 0;
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "block");

        // TODO: re-add errors if fields not found, consider nullable fields again... :\
        if (actvPair == null) {
            throw new XmlPullParserException("Actv not found", parser, null);
        }

        EighthBlock block = new EighthBlock(blockId, date, type, locked);
        return new Pair<>(block, actvPair.second.actvId);
    }

    @NonNull
    static String readBasicDate(@NonNull XmlPullParser parser)
            throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "date");

        String dateStr = "";
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.TEXT && !parser.isWhitespace()) {
                dateStr = parser.getText();
                parser.next();
                break;
            }
            else if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "str":
                    dateStr = readText(parser, "str");
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "date");

        // parse to ensure validity
        try {
            Utils.BASIC_DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Invalid date string: " + dateStr);
            throw new XmlPullParserException("Invalid date string: " + dateStr, parser, e);
        }

        return dateStr;
    }
}
