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
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;

import static com.desklampstudios.thyroxine.eighth.IodineEighthActv.ActivityFlag;

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
    public IodineEighthBlock beginGetBlock(InputStream in)
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
        IodineEighthBlock curBlock = readBlock(mParser);

        // advance to activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "activities");

        return curBlock;
    }

    public IodineEighthBlock nextBlock() throws XmlPullParserException, IOException {
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

    public IodineEighthActv nextActivity() throws XmlPullParserException, IOException {
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

    private static IodineEighthBlock readBlock(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "block");

        int bid = -1;
        long date = 0;
        String type = "";

        IodineEighthActv curActivity = null;
        Boolean locked = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("bid")) {
                bid = readInt(parser, "bid");
            } else if (name.equals("date")) {
                date = readBasicDate(parser);
            } else if (name.equals("type")) {
                type = readText(parser, "type");
            } else if (name.equals("block")) {
                type = readText(parser, "block");
            } else if (name.equals("activity")) {
                curActivity = readActivity(parser);
            } else if (name.equals("locked")) {
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

        return new IodineEighthBlock(bid, date, type, locked, curActivity);
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
        }
        catch (ParseException e) {
            Log.e(TAG, "datetime parse exception: " + dateStr + ", " + e.toString());
        }
        return dateLong;
    }

    private static IodineEighthActv readActivity(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "activity");

        int aid = -1;
        String aName = null;
        String description = null;
        String comment = null;

        BitSet flags = new BitSet();

        String roomsStr = null;
        Integer memberCount = null;
        Integer capacity = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            ActivityFlag flag = ActivityFlag.fromTag(name);

            if (name.equals("aid")) {
                aid = readInt(parser, "aid");
            } else if (name.equals("name")) {
                aName = Html.fromHtml(readText(parser, "name")).toString();
            } else if (name.equals("description")) {
                description = readText(parser, "description");
            } else if (name.equals("comment")) {
                comment = readText(parser, "comment");
            } else if (name.equals("block_rooms_comma")) {
                roomsStr = readText(parser, "block_rooms_comma");
            } else if (name.equals("memberCount")) {
                memberCount = readInt(parser, "memberCount");
            } else if (name.equals("capacity")) {
                capacity = readInt(parser, "capacity");
            } else if (flag != null) {
                flags.set(flag.pos, readInt(parser, flag.tag) != 0);
            } else {
                skip(parser);
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "activity");

        if (aid == -1 || aName == null || description == null) {
            Log.w(TAG, String.format("readActivity: aid (%d) or aName (%s) or description (%s) bad",
                    aid, aName, description));
        }

        return new IodineEighthActv(aid, aName, description, comment,
                flags, roomsStr, memberCount, capacity);
    }
}
