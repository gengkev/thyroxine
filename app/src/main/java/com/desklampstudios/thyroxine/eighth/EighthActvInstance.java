package com.desklampstudios.thyroxine.eighth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class EighthActvInstance {
    public static final int FLAG_ALL = 7168;
    public static final int FLAG_ATTENDANCETAKEN = 1024;
    public static final int FLAG_CANCELLED = 2048;
    //public static final int FLAG_ROOMCHANGED = 4096;

    public final int actvId;
    public final int blockId;
    @NonNull public final String comment;
    public final long flags;

    @NonNull public final String roomsStr;
    public final int memberCount;
    public final int capacity;

    public EighthActvInstance(int actvId, int blockId, @NonNull String comment, long flags,
                              @NonNull String roomsStr, int memberCount, int capacity) {
        this.actvId = actvId;
        this.blockId = blockId;
        this.comment = comment;
        this.flags = flags;
        this.roomsStr = roomsStr;
        this.memberCount = memberCount;
        this.capacity = capacity;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("EighthActvInstance[actvId=%d, blockId=%d, comment=%s, flags=%s, " +
                "roomsStr=%s, memberCount=%d, capacity=%d]",
                actvId, blockId, comment, flags, roomsStr, memberCount, capacity);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof EighthActvInstance)) return false;
        EighthActvInstance other = (EighthActvInstance) o;
        return actvId == other.actvId && blockId == other.blockId && flags == other.flags &&
                memberCount == other.memberCount && capacity == other.capacity &&
                comment.equals(other.comment) && roomsStr.equals(other.roomsStr);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
