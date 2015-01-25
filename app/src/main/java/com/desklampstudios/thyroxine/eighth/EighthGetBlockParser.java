package com.desklampstudios.thyroxine.eighth;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.desklampstudios.thyroxine.AbstractXMLParser;
import com.desklampstudios.thyroxine.AuthErrorParser;
import com.desklampstudios.thyroxine.IodineAuthException;
import com.desklampstudios.thyroxine.Utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static com.desklampstudios.thyroxine.eighth.EighthListBlocksParser.EighthBlockAndActv;

class EighthGetBlockParser extends AbstractXMLParser {
    private static final String TAG = EighthGetBlockParser.class.getSimpleName();

    public EighthGetBlockParser(Context context) throws XmlPullParserException {
        super(context);
    }

    // after this, call nextActivity until it returns null
    @NonNull
    public EighthBlockAndActv beginGetBlock(@NonNull InputStream in)
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

        // getBlock API begins with currently selected block, then all activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "block");
        EighthBlockAndActv blockAndActv = EighthListBlocksParser.readBlock(mParser);

        // advance to activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "activities");

        return blockAndActv;
    }

    // Use with beginGetBlock
    @Nullable
    public Pair<EighthActv, EighthActvInstance> nextActivity()
            throws XmlPullParserException, IOException {
        if (!parsingBegun) {
            return null;
        }

        while (mParser.next() != XmlPullParser.END_TAG) {
            if (mParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (mParser.getName()) {
                case "activity":
                    return readActivity(mParser);
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
                    actvInstanceBuilder.blockId(readInt(parser, "bid"));
                    break;
                case "name":
                    actvBuilder.name(Utils.cleanHtml(readText(parser, "name")));
                    break;
                case "description":
                    actvBuilder.description(Utils.cleanHtml(readText(parser, "description")));
                    break;
                case "comment":
                    actvInstanceBuilder.comment(Utils.cleanHtml(readText(parser, "comment")));
                    break;
                case "block_rooms":
                    actvInstanceBuilder.roomsStr(readBlockRooms(parser));
                    break;
                case "member_count":
                    actvInstanceBuilder.memberCount(readInt(parser, "member_count"));
                    break;
                case "capacity":
                    actvInstanceBuilder.capacity(readInt(parser, "capacity"));
                    break;

                // EighthActv flags
                case "restricted":
                    if (readInt(parser, "restricted") != 0)
                        actvBuilder.withFlag(EighthActv.FLAG_RESTRICTED);
                    break;
                case "sticky":
                    if (readInt(parser, "sticky") != 0)
                        actvBuilder.withFlag(EighthActv.FLAG_STICKY);
                    break;
                case "special":
                    if (readInt(parser, "special") != 0)
                        actvBuilder.withFlag(EighthActv.FLAG_SPECIAL);
                    break;

                // EighthActvInstance flags
                case "attendancetaken":
                    if (readInt(parser, "attendancetaken") != 0)
                        actvInstanceBuilder.withFlag(EighthActvInstance.FLAG_ATTENDANCETAKEN);
                    break;
                case "cancelled":
                    if (readInt(parser, "cancelled") != 0)
                        actvInstanceBuilder.withFlag(EighthActvInstance.FLAG_CANCELLED);
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
}
