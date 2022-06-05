package com.github.kunal52.remote;

import android.content.Context;

import com.github.kunal52.AndroidRemoteContext;
import com.github.kunal52.ssl.KeyStoreManager;
import com.github.kunal52.exception.PairingException;
import com.github.kunal52.ssl.DummyTrustManager;
import com.github.kunal52.wire.PacketParser;

import javax.net.ssl.*;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RemoteSession extends Thread {

    //  private final Logger logger = LoggerFactory.getLogger(RemoteSession.class);

    private final BlockingQueue<Remotemessage.RemoteMessage> mMessageQueue;

    private static RemoteMessageManager mMessageManager;

    private final String mHost;

    private final int mPort;

    private final RemoteSessionListener mRemoteSessionListener;

    private OutputStream outputStream;

    private PacketParser packetParser;

    private final Context mContext;

    public RemoteSession(Context context, String host, int port, RemoteSessionListener remoteSessionListener) {
        mMessageQueue = new LinkedBlockingQueue<>();
        mMessageManager = new RemoteMessageManager();
        AndroidRemoteContext.getInstance().setHost(host);
        mHost = host;
        mPort = port;
        mRemoteSessionListener = remoteSessionListener;
        mContext = context;
    }

    @Override
    public void run() {

        try {
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(new KeyStoreManager(mContext).getKeyManagers(), new TrustManager[]{new DummyTrustManager()}, new SecureRandom());
            SSLSocketFactory sslsocketfactory = sSLContext.getSocketFactory();
            SSLSocket sSLSocket = (SSLSocket) sslsocketfactory.createSocket(mHost, mPort);
            sSLSocket.setNeedClientAuth(true);
            sSLSocket.setUseClientMode(true);
            sSLSocket.setKeepAlive(true);
            sSLSocket.setTcpNoDelay(true);
            sSLSocket.startHandshake();

            outputStream = sSLSocket.getOutputStream();
            packetParser = new RemotePacketParser(sSLSocket.getInputStream(), outputStream, mMessageQueue, new RemoteListener() {
                @Override
                public void onConnected() {
                    mRemoteSessionListener.onConnected();
                }

                @Override
                public void onDisconnected() {

                }

                @Override
                public void onVolume() {

                }

                @Override
                public void onPerformInputDeviceRole() throws PairingException {

                }

                @Override
                public void onPerformOutputDeviceRole(byte[] gamma) throws PairingException {

                }

                @Override
                public void onSessionEnded() {

                }

                @Override
                public void onError(String message) {

                }

                @Override
                public void onLog(String message) {

                }

                @Override
                public void sSLException() {

                }
            });
            packetParser.start();

            Remotemessage.RemoteMessage remoteMessage = waitForMessage();
            //logger.info(remoteMessage.toString());

            byte[] remoteConfigure = mMessageManager.createRemoteConfigure(622, AndroidRemoteContext.getInstance().getModel(), AndroidRemoteContext.getInstance().getVendor(), 1, "1");

            outputStream.write(remoteConfigure);

            waitForMessage();

            byte[] remoteActive = mMessageManager.createRemoteActive(622);
            outputStream.write(remoteActive);
        } catch (SSLException sslException) {
            try {
                mRemoteSessionListener.onSslError();
            } catch (GeneralSecurityException | IOException | InterruptedException | PairingException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mRemoteSessionListener.onError(e.getMessage());
        }
    }

    Remotemessage.RemoteMessage waitForMessage() throws InterruptedException {
        return mMessageQueue.take();
    }

    public void sendCommand(Remotemessage.RemoteKeyCode remoteKeyCode, Remotemessage.RemoteDirection remoteDirection) {
        try {
            outputStream.write(mMessageManager.createKeyCommand(remoteKeyCode, remoteDirection));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void abort() {
        packetParser.abort();
    }

    public interface RemoteSessionListener {
        void onConnected();

        void onSslError() throws GeneralSecurityException, IOException, InterruptedException, PairingException;

        void onDisconnected();

        void onError(String message);
    }

}
