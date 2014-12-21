package com.desklampstudios.thyroxine;

import android.content.Context;

public class IodineAuthException extends Exception {
    public IodineAuthException(String msg) {
        super(msg);
    }

    public static IodineAuthException create(Integer errCode, String msg, Context context) {
        String[] arr = context.getResources().getStringArray(R.array.iodine_auth_error);

        if (msg.trim().equals("You are not logged in.")) {
            return new NotLoggedInException(msg);
        }
        if (errCode != null) {
            switch (errCode) {
                case 1: // bad_password
                    return new InvalidPasswordException(arr[errCode]);
                case 3: // bad_username
                case 4: // tj_email_suffix
                    return new InvalidUsernameException(arr[errCode]);
                case 5: // not_yet_active
                    return new InactiveAccountException(arr[errCode]);
                case 0: // unknown
                case 2: // not_in_database
                    return new IodineAuthException(arr[errCode]);
            }
        }

        return new IodineAuthException(msg);
    }

    public static class NotLoggedInException extends IodineAuthException {
        public NotLoggedInException(String msg) {
            super(msg);
        }
    }
    public static class InvalidPasswordException extends IodineAuthException {
        public InvalidPasswordException(String msg) {
            super(msg);
        }
    }
    public static class InvalidUsernameException extends IodineAuthException {
        public InvalidUsernameException(String msg) {
            super(msg);
        }
    }
    public static class InactiveAccountException extends IodineAuthException {
        public InactiveAccountException(String msg) {
            super(msg);
        }
    }
}