package com.desklampstudios.thyroxine.eighth;

import java.util.Date;

class EighthBlock {
    /* @NotNull */ public int blockId;
    /* @NotNull */ public String date;
    /* @NotNull */ public String type;

    public Boolean locked = null;

    public EighthBlock(int blockId, String date, String type) {
        assert type != null;

        this.blockId = blockId;
        this.date = date;
        this.type = type;
    }

    public EighthBlock(int blockId, String date, String type, Boolean locked) {
        this(blockId, date, type);
        this.locked = locked;
    }

    @Override
    public String toString() {
        return String.format("[Block id: %d, date: %s, type: %s, locked: %s]",
                blockId, date, type, locked);
    }
}
