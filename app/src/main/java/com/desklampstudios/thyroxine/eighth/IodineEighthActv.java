package com.desklampstudios.thyroxine.eighth;

import java.util.BitSet;

class IodineEighthActv {
    public static final int NOT_SELECTED_AID = 999;

    // non-nullable, non-changeable
    public final int aid;

    // required
    public String name;
    public String description;
    public String comment;

    public BitSet flags = null;
    public String roomsStr = null;
    public Integer memberCount = null;
    public Integer capacity = null;

    public IodineEighthActv(int aid, String name, String description, String comment) {
        this.aid = aid;
        this.name = name;
        this.description = description;
        this.comment = comment;
    }

    public IodineEighthActv(int aid, String name, String description, String comment,
                            BitSet flags, String roomsStr, Integer memberCount, Integer capacity) {
        this(aid, name, description, comment);
        this.flags = flags;
        this.roomsStr = roomsStr;
        this.memberCount = memberCount;
        this.capacity = capacity;
    }

    public boolean getFlag(ActivityFlag flag) {
        if (flags == null) return false;
        return flags.get(flag.pos);
    }

    public void setFlag(ActivityFlag flag, boolean bool) {
        if (flags == null) flags = new BitSet();
        flags.set(flag.pos, bool);
    }

    @Override
    public String toString() {
        return String.format("[Activity id: %d, name: %s, flags: %s, description: %s]",
                aid, name, flags, description);
    }

    public enum ActivityFlag {
        SELECTED(0, null),
        RESTRICTED(1, "restricted"),
        PRESIGN(2, "presign"),
        ONEADAY(3, "oneaday"),
        BOTHBLOCKS(4, "bothblocks"),
        STICKY(5, "sticky"),
        SPECIAL(6, "special"),
        CALENDAR(7, "calendar"),
        ATTENDANCETAKEN(8, "attendancetaken"),
        CANCELLED(9, "cancelled"),
        ROOMCHANGED(10, "roomchanged");

        public final int pos;
        public final String tag;

        ActivityFlag(int pos, String tag) {
            this.pos = pos;
            this.tag = tag;
        }

        public static ActivityFlag fromTag(String tag) {
            if (tag == null) return null;
            for (ActivityFlag flag : ActivityFlag.values()) {
                if (flag.tag != null && flag.tag.equals(tag))
                    return flag;
            }
            return null;
        }
    }
}
