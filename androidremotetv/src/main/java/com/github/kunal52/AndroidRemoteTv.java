package com.github.kunal52;

import android.content.Context;

import com.github.kunal52.pairing.PairingListener;
import com.github.kunal52.pairing.PairingSession;
import com.github.kunal52.remote.RemoteSession;
import com.github.kunal52.remote.Remotemessage;

import java.io.File;


public class AndroidRemoteTv extends BaseAndroidRemoteTv {

    //  private final Logger logger = LoggerFactory.getLogger(AndroidRemoteTv.class);
    private final Context mContext;

    private PairingSession mPairingSession;

    private RemoteSession mRemoteSession;

    AndroidRemoteTv(Context context) {
        mContext = context;
    }

    public void connect(String host, AndroidTvListener androidTvListener) {
        mRemoteSession = new RemoteSession(mContext, host, 6466, new RemoteSession.RemoteSessionListener() {
            @Override
            public void onConnected() {
                androidTvListener.onConnected();
            }

            @Override
            public void onSslError() {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onError(String message) {

            }
        });

        if (new File(AndroidRemoteContext.getInstance().getKeyStoreFileName()).exists())
            mRemoteSession.start();
        else {
            mPairingSession = new PairingSession(mContext, new PairingListener() {
                @Override
                public void onSessionCreated() {

                }

                @Override
                public void onPerformInputDeviceRole() {

                }

                @Override
                public void onPerformOutputDeviceRole(byte[] gamma) {

                }

                @Override
                public void onSecretRequested() {
                    androidTvListener.onSecretRequested();
                }

                @Override
                public void onSessionEnded() {

                }

                @Override
                public void onError(String message) {

                }

                @Override
                public void onPaired() {
                    mRemoteSession.start();

                }

                @Override
                public void onLog(String message) {

                }
            });

            mPairingSession.start();
        }

    }

    public void sendCommand(Remotemessage.RemoteKeyCode remoteKeyCode, Remotemessage.RemoteDirection remoteDirection) {
        mRemoteSession.sendCommand(remoteKeyCode, remoteDirection);
    }

    public void abort() {
        if (mRemoteSession != null)
            mRemoteSession.abort();
        if (mPairingSession != null)
            mPairingSession.abort();
    }

    public void sendSecret(String code) {
        mPairingSession.provideSecret(code);
    }

}
