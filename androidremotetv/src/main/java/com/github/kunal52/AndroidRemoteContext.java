package com.github.kunal52;

import java.io.File;
import java.nio.file.Paths;

public class AndroidRemoteContext {

    private String serviceName = "com.github.kunal52/androidTvRemote";

    private String clientName = "androidTvRemoteJava";

    //private File keyStoreFile = Paths.get("androidtv.keystore").toFile();

    private String keyStoreFileName ="androidtv.keystore";

    private char[] keyStorePass = "KeyStore_Password".toCharArray();

    private String host = "";

    private String model = "androidTvRemote";

    private String vendor = "github";


    private static volatile AndroidRemoteContext instance;


    private AndroidRemoteContext() {

    }

    public static AndroidRemoteContext getInstance() {
        AndroidRemoteContext result = instance;
        if (result != null) {
            return result;
        }
        synchronized (AndroidRemoteContext.class) {
            if (instance == null) {
                instance = new AndroidRemoteContext();
            }
            return instance;
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

//    public File getKeyStoreFile() {
//        return keyStoreFile;
//    }

//    public void setKeyStoreFile(File keyStoreFile) {
//        this.keyStoreFile = keyStoreFile;
//    }


    public String getKeyStoreFileName() {
        return keyStoreFileName;
    }

    public void setKeyStoreFileName(String keyStoreFileName) {
        this.keyStoreFileName = keyStoreFileName;
    }

    public char[] getKeyStorePass() {
        return keyStorePass;
    }

    public void setKeyStorePass(char[] keyStorePass) {
        this.keyStorePass = keyStorePass;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}
