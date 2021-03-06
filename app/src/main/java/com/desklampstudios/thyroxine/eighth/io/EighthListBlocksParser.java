package com.desklampstudios.thyroxine.eighth.io;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.Utils;
import com.desklampstudios.thyroxine.eighth.model.EighthActv;
import com.desklampstudios.thyroxine.eighth.model.EighthActvInstance;
import com.desklampstudios.thyroxine.eighth.model.EighthBlock;
import com.desklampstudios.thyroxine.eighth.model.EighthBlockAndActv;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;

class EighthListBlocksParser extends AbstractXMLParser {
    private static final String TAG = EighthListBlocksParser.class.getSimpleName();

    public EighthListBlocksParser(Context context) throws XmlPullParserException {
        super(context);
    }

    public void beginListBlocks(@NonNull InputStream in)
            throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");
    }

    @NonNull
    public ArrayList<EighthBlockAndActv> parseBlocks() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            throw new IllegalStateException();
        }

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "blocks");

        ArrayList<EighthBlockAndActv> blocks = readBlockList(mParser);

        stopParse();
        return blocks;
    }

    @NonNull
    private static ArrayList<EighthBlockAndActv> readBlockList(@NonNull XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "blocks");

        ArrayList<EighthBlockAndActv> blocks = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "block":
                    blocks.add(readBlock(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return blocks;
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
            switch (parser.getName()) {
                case "bid":
                    blockBuilder.blockId(
                            readInt(parser, "bid"));
                    break;
                case "date":
                    blockBuilder.date(
                            readBasicDate(parser));
                    break;
                case "type":
                    blockBuilder.type(
                            readText(parser, "type"));
                    break;
                case "block":
                    blockBuilder.type(
                            readText(parser, "block"));
                    break;
                case "activity":
                    actvPair = EighthGetBlockParser.readActivity(parser);
                    break;
                case "locked":
                    blockBuilder.locked(
                            readBoolean(parser, "locked"));
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
                actvPair.second
        );
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
            throw new XmlPullParserException("Invalid date string: " + dateStr, parser, e);
        }

        return dateStr;
    }
}
