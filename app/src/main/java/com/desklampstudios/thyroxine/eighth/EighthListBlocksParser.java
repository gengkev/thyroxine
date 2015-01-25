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
    public void beginListBlocks(@NonNull InputStream in)
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
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");


        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "blocks");
    }

    // Use with beginListBlocks
    @Nullable
    public EighthBlockAndActv nextBlock() throws XmlPullParserException, IOException {
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
                    break;
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    @NonNull
    static EighthBlockAndActv readBlock(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "block");

        EighthBlock.Builder blockBuilder = new EighthBlock.Builder();
        Pair<EighthActv, EighthActvInstance> actvPair = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "bid":
                    blockBuilder.blockId(readInt(parser, "bid"));
                    break;
                case "date":
                    blockBuilder.date(readBasicDate(parser));
                    break;
                case "type":
                    blockBuilder.type(readText(parser, "type"));
                    break;
                case "block":
                    blockBuilder.type(readText(parser, "block"));
                    break;
                case "activity":
                    actvPair = EighthGetBlockParser.readActivity(parser);
                    break;
                case "locked":
                    blockBuilder.locked(readInt(parser, "locked") != 0);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "block");

        if (actvPair == null) {
            throw new XmlPullParserException("Actv not found", parser, null);
        }

        return new EighthBlockAndActv(
                blockBuilder.build(),
                actvPair.first,
                actvPair.second);
    }

    @NonNull
    private static String readBasicDate(@NonNull XmlPullParser parser)
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
            Utils.FixedDateFormats.BASIC.parse(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, "Invalid date string: " + dateStr);
            throw new XmlPullParserException("Invalid date string: " + dateStr, parser, e);
        }

        return dateStr;
    }

    public static class EighthBlockAndActv {
        @NonNull public final EighthBlock block;
        @NonNull public final EighthActv actv;
        @NonNull public final EighthActvInstance actvInstance;

        public EighthBlockAndActv(@NonNull EighthBlock block,
                                  @NonNull EighthActv actv,
                                  @NonNull EighthActvInstance actvInstance) {
            this.block = block;
            this.actv = actv;
            this.actvInstance = actvInstance;
        }
    }
}
