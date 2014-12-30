package com.desklampstudios.thyroxine.eighth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class EighthBlock {
    public final int blockId;
    @NonNull public final String date;
    @NonNull public final String type;

    public final boolean locked;

    public EighthBlock(int blockId, @NonNull String date, @NonNull String type, boolean locked) {
        this.blockId = blockId;
        this.date = date;
        this.type = type;
        this.locked = locked;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("EighthBlock[blockId=%d, date=%s, type=%s, locked=%s]",
                blockId, date, type, locked);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof EighthBlock)) return false;
        EighthBlock block = (EighthBlock) o;
        return blockId == block.blockId && date.equals(block.date) &&
                type.equals(block.type) && locked == block.locked;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
