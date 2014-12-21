package com.desklampstudios.thyroxine.eighth;

class EighthActvInstance {
    public static final int FLAG_ALL = 7168;
    public static final int FLAG_ATTENDANCETAKEN = 1024;
    public static final int FLAG_CANCELLED = 2048;
    //public static final int FLAG_ROOMCHANGED = 4096;

    /* @NotNull */ public int actvId;
    /* @NotNull */ public int blockId;

    /* @NotNull */ public String comment;
    /* @NotNull */ public long flags;

    public String roomsStr = null;
    public Integer memberCount = null;
    public Integer capacity = null;

    public EighthActvInstance(int actvId, int blockId, String comment, long flags) {
        assert comment != null;

        this.actvId = actvId;
        this.blockId = blockId;
        this.comment = comment;
        this.flags = flags;
    }

    public EighthActvInstance(int actvId, int blockId, String comment, long flags,
                              String roomsStr, Integer memberCount, Integer capacity) {
        this(actvId, blockId, comment, flags);
        this.roomsStr = roomsStr;
        this.memberCount = memberCount;
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return String.format("[ActvInstance actvId: %d, blockId: %d, comment: %s, flags: %s, " +
                "rooms: %s, signed up: %s/%s]",
                actvId, blockId, comment, flags, roomsStr, memberCount, capacity);
    }
}
