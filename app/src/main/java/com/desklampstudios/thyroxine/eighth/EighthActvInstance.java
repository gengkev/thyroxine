package com.desklampstudios.thyroxine.eighth;

class EighthActvInstance {
    public static final int FLAG_ALL = 7168;
    public static final int FLAG_ATTENDANCETAKEN = 1024;
    public static final int FLAG_CANCELLED = 2048;
    //public static final int FLAG_ROOMCHANGED = 4096;

    /* @NotNull */ public final EighthActv actv;

    /* @NotNull */ public String comment;
    /* @NotNull */ public long flags;

    public String roomsStr = null;
    public Integer memberCount = null;
    public Integer capacity = null;

    public EighthActvInstance(EighthActv actv, String comment, long flags) {
        assert comment != null;
        this.actv = actv;
        this.comment = comment;
        this.flags = flags;
    }

    public EighthActvInstance(EighthActv actv, String comment, long flags,
                              String roomsStr, Integer memberCount, Integer capacity) {
        this(actv, comment, flags);
        this.roomsStr = roomsStr;
        this.memberCount = memberCount;
        this.capacity = capacity;
    }

    public long getFlags() {
        return actv.flags | flags;
    }

    @Override
    public String toString() {
        return String.format("[ActvInstance actv: %s, comment: %s, flags: %s, rooms: %s, " +
                "signed up: %s/%s]", actv.toString(), comment, flags, roomsStr, memberCount, capacity);
    }
}
