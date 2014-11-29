package com.desklampstudios.thyroxine.eighth;

import android.text.Html;
import android.util.Log;

import com.desklampstudios.thyroxine.AbstractXMLParser;

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
    private static final DateFormat ISO_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
    private static final DateFormat BASIC_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public IodineEighthParser() throws XmlPullParserException {
        super();
    }

    // after this, call getBlock until it returns null
    public void beginListBlocks(InputStream in) throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);

        mParser.nextTag();
        if (mParser.getName().equals("auth")) {
            throw new IOException("Auth error (not logged in?)");
        }
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "blocks");

        parsingBegun = true;
    }

    // after this, call getActivity until it returns null
    public EighthBlock beginGetBlock(InputStream in)
            throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }
        parsingBegun = true;

        mInputStream = in;
        mParser.setInput(mInputStream, null);

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");

        // getBlock API begins w/ currently selected block, then all activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "block");
        EighthBlock curBlock = readBlock(mParser);

        // advance to activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "activities");

        return curBlock;
    }

    public EighthBlock nextBlock() throws XmlPullParserException, IOException {
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

    public EighthActvInstance nextActivity() throws XmlPullParserException, IOException {
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

    private static EighthBlock readBlock(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "block");

        int bid = -1;
        long date = 0;
        String type = "";

        EighthActvInstance curActivity = null;
        Boolean locked = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("bid")) {
                bid = readInt(parser, "bid");
            }
            else if (name.equals("date")) {
                date = readBasicDate(parser);
            }
            else if (name.equals("type")) {
                type = readText(parser, "type");
            }
            else if (name.equals("block")) {
                type = readText(parser, "block");
            }
            else if (name.equals("activity")) {
                curActivity = readActivity(parser);
            }
            else if (name.equals("locked")) {
                locked = readInt(parser, "locked") != 0;
            } else {
                skip(parser);
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "block");

        if (bid == -1 || date == 0 || type.isEmpty()) {
            Log.w(TAG, String.format("readActivity: bid (%s) or date (%s) or type (%s) bad",
                    bid, date, type));
        }

        return new EighthBlock(bid, date, type, locked, curActivity);
    }

    private static Long readBasicDate(XmlPullParser parser)
            throws IOException, XmlPullParserException {

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

        Long dateLong = null;
        try {
            Date date = BASIC_DATE_FORMAT.parse(dateStr);
            dateLong = date.getTime();
        } catch (ParseException e) {
            Log.e(TAG, "datetime parse exception: " + dateStr + ", " + e.toString());
        }
        return dateLong;
    }

    private static EighthActvInstance readActivity(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "activity");

        int aid = -1;
        String aName = null;
        String description = null;
        String comment = null;
        long flags = 0;

        String roomsStr = null;
        Integer signedUp = null;
        Integer capacity = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            // fields
            if (name.equals("aid")) {
                aid = readInt(parser, "aid");
            }
            else if (name.equals("name")) {
                aName = cleanHtml(readText(parser, "name"));
            }
            else if (name.equals("description")) {
                description = cleanHtml(readText(parser, "description"));
            }
            else if (name.equals("comment")) {
                comment = cleanHtml(readText(parser, "comment"));
            }
            else if (name.equals("block_rooms_comma")) {
                roomsStr = cleanHtml(readText(parser, "block_rooms_comma"));
            }
            else if (name.equals("member_count")) {
                signedUp = readInt(parser, "member_count");
            }
            else if (name.equals("capacity")) {
                capacity = readInt(parser, "capacity");
            }
            // EighthActv flags
            else if (name.equals("restricted")) {
                if (readInt(parser, "restricted") != 0)
                    flags |= EighthActv.FLAG_RESTRICTED;
            }
            else if (name.equals("sticky")) {
                if (readInt(parser, "sticky") != 0)
                    flags |= EighthActv.FLAG_STICKY;
            }
            else if (name.equals("special")) {
                if (readInt(parser, "special") != 0)
                    flags |= EighthActv.FLAG_SPECIAL;
            }
            // EighthActvInstance flags
            else if (name.equals("attendancetaken")) {
                if (readInt(parser, "attendancetaken") != 0)
                    flags |= EighthActvInstance.FLAG_ATTENDANCETAKEN;
            }
            else if (name.equals("cancelled")) {
                if (readInt(parser, "cancelled") != 0)
                    flags |= EighthActvInstance.FLAG_CANCELLED;
            }
            // else
            else {
                skip(parser);
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "activity");

        if (aid == -1 || aName == null || description == null) {
            Log.w(TAG, String.format("readActivity: EighthActv aid (%d) or name (%s) " +
                    "or description (%s) empty", aid, aName, description));
            aName = (aName == null) ? "" : aName;
            description = (description == null) ? "" : description;
        }
        if (comment == null) {
            Log.w(TAG, String.format("readActivity: EighthActvInstance comment (%s) " +
                    "empty", comment));
            comment = (comment == null) ? "" : comment;
        }

        EighthActv actv = new EighthActv(aid, aName, description, flags & EighthActv.FLAG_ALL);

        return new EighthActvInstance(actv, comment,
                flags & EighthActvInstance.FLAG_ALL, roomsStr, signedUp, capacity);
    }

    private static String cleanHtml(String in) {
        return Html.fromHtml(in).toString().trim();
    }
}
