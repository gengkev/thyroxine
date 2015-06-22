package com.desklampstudios.thyroxine.eighth.model;

import android.support.annotation.NonNull;

public class EighthBlockAndActv {
    @NonNull public final EighthBlock block;
    @NonNull public final EighthActv actv;
    @NonNull public final EighthActvInstance actvInstance;

    public EighthBlockAndActv(@NonNull EighthBlock block,
                              @NonNull EighthActv actv,
                              @NonNull EighthActvInstance actvInstance) {
        this.block = block;
        this.actv = actv;
        this.actvInstance = actvInstance;
    }
}
