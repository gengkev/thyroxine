package com.desklampstudios.thyroxine.eighth;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.IodineAuthErrorParser;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class IodineEighthParser extends AbstractXMLParser {
    private static final String TAG = IodineEighthParser.class.getSimpleName();

    public IodineEighthParser(Context context) throws XmlPullParserException {
        super(context);
    }

    // after this, call getBlock until it returns null
    public void beginListBlocks(InputStream in) throws XmlPullParserException, IOException, IodineAuthException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;

        mParser.nextTag();
        if (mParser.getName().equals("auth")) { // Auth error
            throw IodineAuthErrorParser.getErrorStatic(this);
        }
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "blocks");
    }

    // Use with beginListBlocks
    public Pair<EighthBlock, Integer> nextBlock() throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = mParser.getName();
            if (name.equals("block")) {
                return readBlock(mParser);
            } else {
                skip(mParser);
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    // after this, call nextActivity until it returns null
    public Pair<EighthBlock, Integer> beginGetBlock(InputStream in)
            throws XmlPullParserException, IOException, IodineAuthException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;

        mParser.nextTag();
        if (mParser.getName().equals("auth")) { // Auth error
            throw IodineAuthErrorParser.getErrorStatic(this);
        }
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");

        // getBlock API begins with currently selected block, then all activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "block");
        Pair<EighthBlock, Integer> pair = readBlock(mParser);

        // advance to activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "activities");

        return pair;
    }

    // Use with beginGetBlock
    public Pair<EighthActv, EighthActvInstance> nextActivity()
            throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = mParser.getName();
            if (name.equals("activity")) {
                return readActivity(mParser);
            } else {
                skip(mParser);
            }
        }

        // No more entries found
        stopParse();
        return null;
    }

    private static Pair<EighthBlock, Integer> readBlock(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "block");

        int bid = -1;
        String date = "";
        String type = "";
        Boolean locked = null;

        Pair<EighthActv, EighthActvInstance> actvPair = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
            case "bid":
                bid = readInt(parser, "bid");
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
                actvPair = readActivity(parser);
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

        // Check for valid inputs
        if (bid < 0 || date.isEmpty() || type.isEmpty()) {
            String msg = String.format("readBlock: bid (%s) or date (%s) or type (%s) bad",
                    bid, date, type);
            Log.e(TAG, msg);
            throw new XmlPullParserException(msg, parser, null);
        }
        if (actvPair == null) {
            String msg = "readBlock: activity not found";
            Log.e(TAG, msg);
            throw new XmlPullParserException(msg, parser, null);
        }

        EighthBlock block = new EighthBlock(bid, date, type, locked);
        return new Pair<>(block, actvPair.second.blockId);
    }

    private static String readBasicDate(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "date");

        String dateStr = "";
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.TEXT && !parser.isWhitespace()) {
                dateStr = parser.getText();
                parser.next();
                break;
            }

            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("str")) {
                dateStr = readText(parser, "str");
            } else {
                skip(parser);
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

    private static Pair<EighthActv, EighthActvInstance> readActivity(XmlPullParser parser)
            throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "activity");

        int aid = -1;
        String aName = null;
        String description = null;
        String comment = null;
        long flags = 0;

        String roomsStr = null;
        Integer memberCount = null;
        Integer capacity = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            // fields
            switch (name) {
                case "aid":
                    aid = readInt(parser, "aid");
                    break;
                case "name":
                    aName = Utils.cleanHtml(readText(parser, "name"));
                    break;
                case "description":
                    description = Utils.cleanHtml(readText(parser, "description"));
                    break;
                case "comment":
                    comment = Utils.cleanHtml(readText(parser, "comment"));
                    break;
                case "block_rooms_comma":
                    roomsStr = Utils.cleanHtml(readText(parser, "block_rooms_comma"));
                    break;
                case "member_count":
                    memberCount = readInt(parser, "member_count");
                    break;
                case "capacity":
                    capacity = readInt(parser, "capacity");
                    break;

                // EighthActv flags
                case "restricted":
                    if (readInt(parser, "restricted") != 0)
                        flags |= EighthActv.FLAG_RESTRICTED;
                    break;
                case "sticky":
                    if (readInt(parser, "sticky") != 0)
                        flags |= EighthActv.FLAG_STICKY;
                    break;
                case "special":
                    if (readInt(parser, "special") != 0)
                        flags |= EighthActv.FLAG_SPECIAL;
                    break;

                // EighthActvInstance flags
                case "attendancetaken":
                    if (readInt(parser, "attendancetaken") != 0)
                        flags |= EighthActvInstance.FLAG_ATTENDANCETAKEN;
                    break;
                case "cancelled":
                    if (readInt(parser, "cancelled") != 0)
                        flags |= EighthActvInstance.FLAG_CANCELLED;
                    break;

                // else
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "activity");

        // Check for valid inputs
        if (aid < 0 || aName == null || description == null) {
            String msg = String.format("readActivity: EighthActv aid (%d) or name (%s) " +
                    "or description (%s) not found", aid, aName, description);
            Log.e(TAG, msg);
            throw new XmlPullParserException(msg, parser, null);
        }
        if (comment == null) {
            String msg = String.format("readActivity: EighthActvInstance comment (%s) " +
                    "not found", comment);
            Log.e(TAG, msg);
            throw new XmlPullParserException(msg, parser, null);
        }

        EighthActv actv = new EighthActv(aid, aName, description,
                flags & EighthActv.FLAG_ALL);

        EighthActvInstance actvInstance = new EighthActvInstance(aid, -1, comment,
                flags & EighthActvInstance.FLAG_ALL, roomsStr, memberCount, capacity);

        return new Pair<>(actv, actvInstance);
    }
}
