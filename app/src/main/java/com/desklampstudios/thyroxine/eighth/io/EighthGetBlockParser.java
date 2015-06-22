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
import java.util.ArrayList;

class EighthGetBlockParser extends AbstractXMLParser {
    private static final String TAG = EighthGetBlockParser.class.getSimpleName();

    private EighthBlock mBlock;

    public EighthGetBlockParser(Context context) throws XmlPullParserException {
        super(context);
    }

    @NonNull
    public EighthBlockAndActv beginGetBlock(@NonNull InputStream in)
            throws XmlPullParserException, IOException {
        if (parsingBegun) {
            stopParse();
        }

        mInputStream = in;
        mParser.setInput(mInputStream, null);
        parsingBegun = true;

        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");

        // getBlock API begins with currently selected block, then all activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "block");

        EighthBlockAndActv blockAndActv = EighthListBlocksParser.readBlock(mParser);
        mBlock = blockAndActv.block;
        return blockAndActv;
    }

    @NonNull
    public ArrayList<EighthBlockAndActv> parseActivities()
            throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            throw new IllegalStateException();
        }

        // advance to activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "activities");

        ArrayList<EighthBlockAndActv> actvs = readActivityList(mParser, mBlock);

        stopParse();
        return actvs;
    }

    @NonNull
    private static ArrayList<EighthBlockAndActv> readActivityList(XmlPullParser parser, EighthBlock block)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "activities");

        ArrayList<EighthBlockAndActv> activities = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "activity":
                    Pair<EighthActv, EighthActvInstance> actvPair = readActivity(parser);
                    EighthBlockAndActv blockAndActv = new EighthBlockAndActv(
                            block, actvPair.first, actvPair.second);
                    activities.add(blockAndActv);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return activities;
    }

    @NonNull
    static Pair<EighthActv, EighthActvInstance> readActivity(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "activity");

        EighthActv.Builder actvBuilder = new EighthActv.Builder();
        EighthActvInstance.Builder actvInstanceBuilder = new EighthActvInstance.Builder();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            switch (parser.getName()) {
                // fields
                case "aid": {
                    int actvId = readInt(parser, "aid");
                    actvBuilder.actvId(actvId);
                    actvInstanceBuilder.actvId(actvId);
                    break;
                }
                case "bid":
                    actvInstanceBuilder.blockId(
                            readInt(parser, "bid"));
                    break;
                case "name":
                    actvBuilder.name(
                            Utils.cleanHtml(readText(parser, "name")));
                    break;
                case "description":
                    actvBuilder.description(
                            Utils.cleanHtml(readText(parser, "description")));
                    break;
                case "comment":
                    actvInstanceBuilder.comment(
                            Utils.cleanHtml(readText(parser, "comment")));
                    break;
                case "block_rooms":
                    actvInstanceBuilder.roomsStr(
                            readBlockRooms(parser));
                    break;
                case "block_sponsors":
                    actvInstanceBuilder.sponsorsStr(
                            readBlockSponsors(parser));
                    break;
                case "member_count":
                    actvInstanceBuilder.memberCount(
                            readInt(parser, "member_count"));
                    break;
                case "capacity":
                    actvInstanceBuilder.capacity(
                            readInt(parser, "capacity"));
                    break;

                // EighthActv flags
                case "restricted":
                    actvBuilder.withFlag(EighthActv.FLAG_RESTRICTED,
                            readBoolean(parser, "restricted"));
                    break;
                case "presign":
                    actvBuilder.withFlag(EighthActv.FLAG_PRESIGN,
                            readBoolean(parser, "presign"));
                    break;
                case "oneaday":
                    actvBuilder.withFlag(EighthActv.FLAG_ONEADAY,
                            readBoolean(parser, "oneaday"));
                    break;
                case "bothblocks":
                    actvBuilder.withFlag(EighthActv.FLAG_BOTHBLOCKS,
                            readBoolean(parser, "bothblocks"));
                    break;
                case "sticky":
                    actvBuilder.withFlag(EighthActv.FLAG_STICKY,
                            readBoolean(parser, "sticky"));
                    break;
                case "special":
                    actvBuilder.withFlag(EighthActv.FLAG_SPECIAL,
                            readBoolean(parser, "special"));
                    break;

                // EighthActvInstance flags
                case "attendancetaken":
                    actvInstanceBuilder.withFlag(EighthActvInstance.FLAG_ATTENDANCETAKEN,
                            readBoolean(parser, "attendancetaken"));
                    break;
                case "cancelled":
                    actvInstanceBuilder.withFlag(EighthActvInstance.FLAG_CANCELLED,
                            readBoolean(parser, "cancelled"));
                    break;

                // else
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "activity");

        return new Pair<>(
                actvBuilder.build(),
                actvInstanceBuilder.build());
    }

    // TODO: maybe actually get rooms at some point??
    @NonNull
    private static String readBlockRooms(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "block_rooms");

        ArrayList<String> rooms = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "room":
                    rooms.add(readRoom(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "block_rooms");

        return Utils.join(rooms, ", ");
    }
    @NonNull
    private static String readRoom(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "room");

        String name = "";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "name":
                    name = Utils.cleanHtml(readText(parser, "name"));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return name;
    }

    @NonNull
    private static String readBlockSponsors(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "block_sponsors");

        ArrayList<String> sponsors = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "sponsor":
                    sponsors.add(readSponsor(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        parser.require(XmlPullParser.END_TAG, ns, "block_sponsors");

        return Utils.join(sponsors, ", ");
    }
    @NonNull
    private static String readSponsor(@NonNull XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "sponsor");

        String fname = "";
        String lname = "";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case "fname":
                    fname = Utils.cleanHtml(readText(parser, "fname"));
                    break;
                case "lname":
                    lname = Utils.cleanHtml(readText(parser, "lname"));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        // Assuming that both aren't empty
        if (fname.isEmpty()) {
            return lname;
        } else if (lname.isEmpty()) {
            return fname;
        }
        return String.format("%s, %s.", lname, fname.substring(0, 1));
    }
}
