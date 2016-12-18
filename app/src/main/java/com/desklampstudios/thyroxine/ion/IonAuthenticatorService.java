package com.desklampstudios.thyroxine.ion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A bound Service that instantiates the authenticator
 * when started.
 */
public class IonAuthenticatorService extends Service {
    private IonAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new IonAuthenticator(this);
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
