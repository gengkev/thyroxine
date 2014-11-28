package com.desklampstudios.thyroxine;

public class IodineAuthException extends Exception {
    public static final int MAX_ERRCODE = 5;
    public final int errCode;

    public IodineAuthException(Integer errCode) {
        super("Error code " + errCode);
        if (errCode != null && 0 <= errCode && errCode <= MAX_ERRCODE)
            this.errCode = errCode;
        else
            this.errCode = 0;
    }
}
