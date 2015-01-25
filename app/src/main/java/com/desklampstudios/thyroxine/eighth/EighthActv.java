package com.desklampstudios.thyroxine.eighth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class EighthActv implements Comparable<EighthActv> {
    public static final int NOT_SELECTED_AID = 999;

    public static final int FLAG_ALL = 127;
    public static final int FLAG_RESTRICTED = 1;
    public static final int FLAG_PRESIGN = 2;
    public static final int FLAG_ONEADAY = 4;
    public static final int FLAG_BOTHBLOCKS = 8;
    public static final int FLAG_STICKY = 16;
    public static final int FLAG_SPECIAL = 32;
    //public static final int FLAG_CALENDAR = 64;

    public final int actvId;
    @NonNull public final String name;
    @NonNull public final String description;
    public final long flags;

    private EighthActv(Builder builder) {
        this.actvId = builder.actvId;
        this.name = builder.name;
        this.description = builder.description;
        this.flags = builder.flags;
    }

    public boolean getFlag(long flag) {
        return (this.flags & flag) != 0;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("EighthActv[actvId=%d, name=%s, description=%s, flags=%d]",
                actvId, name, description, flags);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof EighthActv)) return false;
        EighthActv actv = (EighthActv) o;
        return actvId == actv.actvId && name.equals(actv.name) &&
                description.equals(actv.description) && flags == actv.flags;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(@NonNull EighthActv other) {
        int cmp;
        // list special first
        if ((cmp = Boolean.valueOf(getFlag(EighthActv.FLAG_SPECIAL)).compareTo(
                other.getFlag(EighthActv.FLAG_SPECIAL))) != 0) return cmp;
        // sort by name
        if ((cmp = name.compareTo(other.name)) != 0) return cmp;
        return 0;
    }

    public static class Builder {
        private int actvId = -1;
        @NonNull private String name = "";
        @NonNull private String description = "";
        private long flags = 0;

        public Builder() {}

        public Builder actvId(int actvId) {
            this.actvId = actvId;
            return this;
        }
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }
        public Builder description(@NonNull String description) {
            this.description = description;
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

        public EighthActv build() {
            EighthActv actv = new EighthActv(this);
            if ((actv.flags & ~FLAG_ALL) != 0) {
                throw new IllegalStateException("Flags invalid: " + actv.flags);
            }
            return actv;
        }
    }
}
