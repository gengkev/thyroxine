package com.desklampstudios.thyroxine.eighth.io;

import android.content.Context;
import android.support.annotation.NonNull;

import com.desklampstudios.thyroxine.R;
import com.desklampstudios.thyroxine.Utils;

import java.util.ArrayList;

public class EighthSignupException extends Exception {
    private EighthSignupException(String msg) {
        super(msg);
    }

    @NonNull
    public static EighthSignupException create(int errCode, @NonNull Context context) {
        final String[] arr = context.getResources().getStringArray(R.array.eighth_signup_error);

        ArrayList<String> messages = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            if ((errCode & (1 << i)) != 0) {
                messages.add(arr[i]);
            }
        }

        if (messages.isEmpty()) {
            return new EighthSignupException(
                    context.getString(R.string.unexpected_error, "(" + errCode + ")"));
        } else {
            return new EighthSignupException(Utils.join(messages, "\n"));
        }
    }
}
