package com.desklampstudios.thyroxine.iodine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A bound Service that instantiates the authenticator
 * when started.
 */
public class IodineAuthenticatorService extends Service {
    private IodineAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new IodineAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
