package com.desklampstudios.thyroxine.eighth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class EighthActv {
    public static final int NOT_SELECTED_AID = 999;

    public static final int FLAG_ALL = 127;
    public static final int FLAG_RESTRICTED = 1;
    //public static final int FLAG_PRESIGN = 2;
    //public static final int FLAG_ONEADAY = 4;
    //public static final int FLAG_BOTHBLOCKS = 8;
    public static final int FLAG_STICKY = 16;
    public static final int FLAG_SPECIAL = 32;
    //public static final int FLAG_CALENDAR = 64;

    public final int actvId;
    @NonNull public final String name;
    @NonNull public final String description;
    public final long flags;

    public EighthActv(int actvId, @NonNull String name, @NonNull String description, long flags) {
        this.actvId = actvId;
        this.name = name;
        this.description = description;
        this.flags = flags;
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
}
