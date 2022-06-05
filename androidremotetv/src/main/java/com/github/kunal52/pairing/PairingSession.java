package com.github.kunal52.pairing;

import android.content.Context;

import com.github.kunal52.AndroidRemoteContext;
import com.github.kunal52.exception.PairingException;
import com.github.kunal52.ssl.DummyTrustManager;
import com.github.kunal52.ssl.KeyStoreManager;
import com.github.kunal52.util.Utils;


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PairingSession extends Thread {

    //private final Logger logger = LoggerFactory.getLogger(PairingSession.class);

    private final BlockingQueue<Pairingmessage.PairingMessage> mMessagesQueue;

    private final PairingMessageManager mPairingMessageManager;

    private String mPairingSecret;

    SecretProvider secretProvider;

    private SSLSocket mSslSocket;

    private PairingPacketParser pairingPacketParser;

    private final PairingListener mPairingListener;

    private final Context mContext;

    private final ReentrantLock lock;
    private final Condition condition;

    public PairingSession(Context context, PairingListener pairingListener) {
        mMessagesQueue = new LinkedBlockingDeque<>();
        mPairingMessageManager = new PairingMessageManager();
        mPairingListener = pairingListener;
        mContext = context;
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    @Override
    public void run() {

        try {
            lock.lock();
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(new KeyStoreManager(mContext).getKeyManagers(), new TrustManager[]{new DummyTrustManager()}, new SecureRandom());
            SSLSocketFactory sslsocketfactory = sSLContext.getSocketFactory();
            SSLSocket sSLSocket = (SSLSocket) sslsocketfactory.createSocket(AndroidRemoteContext.getInstance().getHost(), 6467);
            mSslSocket = sSLSocket;

            mPairingListener.onSessionCreated();
            pairingPacketParser = new PairingPacketParser(sSLSocket.getInputStream(), mMessagesQueue);
            pairingPacketParser.start();

            final OutputStream outputStream = sSLSocket.getOutputStream();

            byte[] pairingMessage = mPairingMessageManager.createPairingMessage(AndroidRemoteContext.getInstance().getClientName(), AndroidRemoteContext.getInstance().getServiceName());
            outputStream.write(pairingMessage);
            Pairingmessage.PairingMessage pairingMessageResponse = waitForMessage();
            logReceivedMessage(pairingMessageResponse.toString());

            byte[] pairingOption = new PairingMessageManager().createPairingOption();
            outputStream.write(pairingOption);
            Pairingmessage.PairingMessage pairingOptionAck = waitForMessage();
            logReceivedMessage(pairingOptionAck.toString());

            byte[] configMessage = new PairingMessageManager().createConfigMessage();
            outputStream.write(configMessage);
            Pairingmessage.PairingMessage pairingConfigAck = waitForMessage();
            logReceivedMessage(pairingConfigAck.toString());

            if (secretProvider != null)
                secretProvider.requestSecret(this);
            mPairingListener.onSecretRequested();
            //logger.info("Waiting for secret");
            condition.await();
            byte[] secretMessage = mPairingMessageManager.createSecretMessage(createCodeSecret());
            outputStream.write(secretMessage);
            Pairingmessage.PairingMessage pairingSecretAck = waitForMessage();
            logReceivedMessage(pairingSecretAck.toString());

            mPairingListener.onPaired();
            mPairingListener.onSessionEnded();

        } catch (GeneralSecurityException | IOException | InterruptedException | PairingException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    Pairingmessage.PairingMessage waitForMessage() throws InterruptedException, PairingException {
        Pairingmessage.PairingMessage pairingMessage = mMessagesQueue.take();
        if (pairingMessage.getStatus() != Pairingmessage.PairingMessage.Status.STATUS_OK) {
            throw new PairingException(pairingMessage.toString());
        }
        return pairingMessage;
    }

    public void provideSecret(String secret) {
        lock.lock();
        mPairingSecret = secret;
        condition.signal();
        lock.unlock();
    }

    private Pairingmessage.PairingMessage createCodeSecret() {
        mPairingSecret = mPairingSecret.substring(2);
        PairingChallengeResponse pairingChallengeResponse = new PairingChallengeResponse(Utils.getLocalCert(mSslSocket.getSession()), Utils.getPeerCert(mSslSocket.getSession()));
        byte[] secret = Utils.hexStringToBytes(mPairingSecret);
        System.out.println(Arrays.toString(secret));
        try {
            pairingChallengeResponse.checkGamma(secret);
        } catch (PairingException e) {
            throw new RuntimeException(e);
        }
        byte[] pairingChallengeResponseAlpha;
        try {
            pairingChallengeResponseAlpha = pairingChallengeResponse.getAlpha(secret);
        } catch (PairingException e) {
            throw new RuntimeException(e);
        }

        return mPairingMessageManager.createSecretMessageProto(pairingChallengeResponseAlpha);
    }

    public void abort() {
        if (pairingPacketParser != null)
            pairingPacketParser.abort();
    }

    void logSendMessage(String message) {
        //logger.info("Send Message : {}", message);
    }

    void logReceivedMessage(String message) {
        //logger.info("Received Message : {}", message);
    }

}
