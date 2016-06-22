package com.desklampstudios.thyroxine.ion;


public class IonAuthException extends Exception {
    public IonAuthException(String message) {
        super(message);
    }

    public static class NotLoggedInException extends IonAuthException {
        public NotLoggedInException(String msg) {
            super(msg);
        }
    }
    public static class InactiveAccountException extends IonAuthException {
        public InactiveAccountException(String msg) {
            super(msg);
        }
    }
}
