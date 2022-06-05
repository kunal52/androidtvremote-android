package com.github.kunal52;

import java.io.File;

abstract class BaseAndroidRemoteTv {

    private final AndroidRemoteContext androidRemoteContext;

    BaseAndroidRemoteTv() {
        androidRemoteContext = AndroidRemoteContext.getInstance();
    }


    public String getServiceName() {
        return androidRemoteContext.getServiceName();
    }

    public void setServiceName(String serviceName) {
        androidRemoteContext.setServiceName(serviceName);
    }

    public String getClientName() {
        return androidRemoteContext.getClientName();
    }

    public void setClientName(String clientName) {
        androidRemoteContext.setClientName(clientName);
    }

    public char[] getKeyStorePass() {
        return androidRemoteContext.getKeyStorePass();
    }

    public void setKeyStorePass(String keyStorePass) {
        androidRemoteContext.setKeyStorePass(keyStorePass.toCharArray());
    }
}
