package com.desklampstudios.thyroxine.eighth;

class EighthActvInstance {
    public static final int FLAG_ALL = 7168;
    public static final int FLAG_ATTENDANCETAKEN = 1024;
    public static final int FLAG_CANCELLED = 2048;
    //public static final int FLAG_ROOMCHANGED = 4096;

    public int actvId;
    public int blockId;
    public String comment;
    public long flags;

    public String roomsStr = "";
    public int memberCount = 0;
    public int capacity = -1;

    public EighthActvInstance(int actvId, int blockId, String comment, long flags) {
        assert comment != null;

        this.actvId = actvId;
        this.blockId = blockId;
        this.comment = comment;
        this.flags = flags;
    }

    public EighthActvInstance(int actvId, int blockId, String comment, long flags,
                              String roomsStr, int memberCount, int capacity) {
        this(actvId, blockId, comment, flags);
        assert roomsStr != null;

        this.roomsStr = roomsStr;
        this.memberCount = memberCount;
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return String.format("EighthActvInstance[actvId=%d, blockId=%d, comment=%s, flags=%s, " +
                "roomsStr=%s, memberCount=%d, capacity=%d]",
                actvId, blockId, comment, flags, roomsStr, memberCount, capacity);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
