package com.desklampstudios.thyroxine.eighth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.desklampstudios.thyroxine.Utils;

import java.text.ParseException;

class EighthBlock {
    public final int blockId;
    @NonNull public final String date;
    @NonNull public final String type;
    public final boolean locked;

    private EighthBlock(Builder builder) {
        this.blockId = builder.blockId;
        this.date = builder.date;
        this.type = builder.type;
        this.locked = builder.locked;
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

    public static class Builder {
        private int blockId = -1;
        @NonNull private String date = "";
        @NonNull private String type = "";
        private boolean locked = false;

        public Builder() {
        }

        public Builder blockId(int blockId) {
            this.blockId = blockId;
            return this;
        }
        public Builder date(@NonNull String date) {
            this.date = date;
            return this;
        }
        public Builder type(@NonNull String type) {
            this.type = type;
            return this;
        }
        public Builder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public EighthBlock build() {
            EighthBlock block = new EighthBlock(this);
            try {
                Utils.FixedDateFormats.BASIC.parse(block.date);
            } catch (ParseException e) {
                throw new IllegalStateException("Date invalid: " + block.date);
            }
            return block;
        }
    }
}
