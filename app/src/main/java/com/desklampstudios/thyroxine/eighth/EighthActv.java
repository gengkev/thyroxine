package com.desklampstudios.thyroxine.eighth;

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

    /* @NotNull */ public int actvId;

    /* @NotNull */ public String name;
    /* @NotNull */ public String description;
    /* @NotNull */ public long flags;

    public EighthActv(int actvId, String name, String description, long flags) {
        assert name != null;
        assert description != null;

        this.actvId = actvId;
        this.name = name;
        this.description = description;
        this.flags = flags;
    }

    @Override
    public String toString() {
        return String.format("[Actv id: %d, name: %s, description: %s, flags: %d]",
                actvId, name, description, flags);
    }
}
