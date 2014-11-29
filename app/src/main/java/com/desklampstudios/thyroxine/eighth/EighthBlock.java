package com.desklampstudios.thyroxine.eighth;

import java.util.Date;

class EighthBlock {
    /* @NotNull */ public final int bid;
    /* @NotNull */ public final long date;
    /* @NotNull */ public final String type;

    public Boolean locked = null;
    public EighthActvInstance selectedActv = null;

    public EighthBlock(int bid, long date, String type) {
        assert type != null;

        this.bid = bid;
        this.date = date;
        this.type = type;
    }

    public EighthBlock(int bid, long date, String type,
                       Boolean locked, EighthActvInstance selectedActv) {
        this(bid, date, type);
        this.locked = locked;
        this.selectedActv = selectedActv;
    }

    @Override
    public String toString() {
        return String.format("[Block id: %d, date: %s, type: %s, locked: %s]",
                bid, new Date(date), type, locked);
    }
}
