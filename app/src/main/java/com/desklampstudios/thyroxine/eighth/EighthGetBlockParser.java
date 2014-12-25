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

class EighthGetBlockParser extends AbstractXMLParser {
    private static final String TAG = EighthGetBlockParser.class.getSimpleName();

    private int curBlockId = -1;

    public EighthGetBlockParser(Context context) throws XmlPullParserException {
        super(context);
    }

    // after this, call nextActivity until it returns null
    @NonNull
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
            throw AuthErrorParser.readAuth(mParser, mContext);
        }
        mParser.require(XmlPullParser.START_TAG, ns, "eighth");

        // getBlock API begins with currently selected block, then all activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "block");
        Pair<EighthBlock, Integer> pair = EighthListBlocksParser.readBlock(mParser);
        curBlockId = pair.first.blockId;

        // advance to activities
        mParser.nextTag();
        mParser.require(XmlPullParser.START_TAG, ns, "activities");

        return pair;
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
                    Pair<EighthActv, EighthActvInstance> pair = readActivity(mParser);
                    pair.second.blockId = curBlockId;
                    return pair;
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

        int actvId = -1;
        String actvName = "";
        String description = "";
        String comment = "";
        long flags = 0;

        String roomsStr = "";
        int memberCount = 0;
        int capacity = -1;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            // fields
            switch (name) {
                case "aid":
                    actvId = readInt(parser, "aid");
                    break;
                case "name":
                    actvName = Utils.cleanHtml(readText(parser, "name"));
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

        // TODO: re-add errors if fields not found, consider nullable fields again... :\

        EighthActv actv = new EighthActv(actvId, actvName, description,
                flags & EighthActv.FLAG_ALL);

        EighthActvInstance actvInstance = new EighthActvInstance(actvId, -1, comment,
                flags & EighthActvInstance.FLAG_ALL, roomsStr, memberCount, capacity);

        return new Pair<>(actv, actvInstance);
    }
}
