package com.desklampstudios.thyroxine.eighth;

class EighthBlock {
    public int blockId;
    public String date;
    public String type;

    public boolean locked = false;

    public EighthBlock(int blockId, String date, String type) {
        assert date != null;
        assert type != null;

        this.blockId = blockId;
        this.date = date;
        this.type = type;
    }

    public EighthBlock(int blockId, String date, String type, boolean locked) {
        this(blockId, date, type);
        this.locked = locked;
    }

    @Override
    public String toString() {
        return String.format("EighthBlock[blockId=%d, date=%s, type=%s, locked=%s]",
                blockId, date, type, locked);
    }

    @Override
    public boolean equals(Object o) {
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
