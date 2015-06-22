package com.desklampstudios.thyroxine.eighth.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class EighthActvInstance {
    public static final int FLAG_ALL = 7168;
    public static final int FLAG_ATTENDANCETAKEN = 1024;
    public static final int FLAG_CANCELLED = 2048;
    //public static final int FLAG_ROOMCHANGED = 4096;

    public final int actvId;
    public final int blockId;
    @NonNull public final String comment;
    public final long flags;
    @NonNull public final String roomsStr;
    @NonNull public final String sponsorsStr;
    public final int memberCount;
    public final int capacity;

    public EighthActvInstance(Builder builder) {
        this.actvId = builder.actvId;
        this.blockId = builder.blockId;
        this.comment = builder.comment;
        this.flags = builder.flags;
        this.roomsStr = builder.roomsStr;
        this.sponsorsStr = builder.sponsorsStr;
        this.memberCount = builder.memberCount;
        this.capacity = builder.capacity;
    }

    public boolean getFlag(long flag) {
        return (this.flags & flag) != 0;
    }

    public boolean isFull() {
        return capacity > 0 && memberCount >= capacity;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("EighthActvInstance[actvId=%d, blockId=%d, comment=%s, flags=%s, " +
                "roomsStr=%s, sponsorsStr=%s, memberCount=%d, capacity=%d]",
                actvId, blockId, comment, flags, roomsStr, sponsorsStr, memberCount, capacity);
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

    public static class Builder {
        private int actvId = -1;
        private int blockId = -1;
        @NonNull private String comment = "";
        private long flags = 0;
        @NonNull private String roomsStr = "";
        @NonNull private String sponsorsStr = "";
        private int memberCount = 0;
        private int capacity = -1;

        public Builder() {}

        public Builder actvId(int actvId) {
            this.actvId = actvId;
            return this;
        }
        public Builder blockId(int blockId) {
            this.blockId = blockId;
            return this;
        }
        public Builder comment(@NonNull String comment) {
            this.comment = comment;
            return this;
        }
        public Builder flags(long flags) {
            this.flags = flags;
            return this;
        }
        public Builder withFlag(long flag) {
            this.flags |= flag;
            return this;
        }
        public Builder withFlag(long flag, boolean set) {
            if (set) {
                this.flags |= flag;
            } else {
                this.flags &= ~flag;
            }
            return this;
        }
        public Builder roomsStr(@NonNull String roomsStr) {
            this.roomsStr = roomsStr;
            return this;
        }
        public Builder sponsorsStr(@NonNull String sponsorsStr) {
            this.sponsorsStr = sponsorsStr;
            return this;
        }
        public Builder memberCount(int memberCount) {
            this.memberCount = memberCount;
            return this;
        }
        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public EighthActvInstance build() {
            EighthActvInstance actvInstance = new EighthActvInstance(this);
            if ((actvInstance.flags & ~FLAG_ALL) != 0) {
                throw new IllegalStateException("Flags invalid: " + actvInstance.flags);
            }
            return actvInstance;
        }
    }
}
