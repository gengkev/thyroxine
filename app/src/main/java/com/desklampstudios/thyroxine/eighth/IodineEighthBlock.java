package com.desklampstudios.thyroxine.eighth;

import java.util.Date;

class IodineEighthBlock {
    // non-nullable, non-changeable
    public final int bid;
    public final long date;
    public final String type;

    public Boolean locked = null;
    public IodineEighthActv currentActv = null;

    public IodineEighthBlock(int bid, long date, String type) {
        this.bid = bid;
        this.date = date;
        this.type = type;
    }
    public IodineEighthBlock(int bid, long date, String type,
                             Boolean locked, IodineEighthActv currentActv) {
        this(bid, date, type);

        this.locked = locked;
        this.currentActv = currentActv;
    }

    @Override
    public String toString() {
        return String.format("[Block id: %d, date: %s, type: %s, locked: %s]",
                bid, new Date(date), type, locked);
    }
}
