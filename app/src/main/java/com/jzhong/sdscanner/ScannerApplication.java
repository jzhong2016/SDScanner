package com.jzhong.sdscanner;

import android.app.Application;

/**
 * Created by busyzhong on 9/10/16.
 */
public class ScannerApplication extends Application{

    public static ScannerApplication application;

    private FileScanner fileScanner;

    public ScannerApplication() {
        application = this;
    }

    public FileScanner getFileScanner() {
        if(fileScanner==null) {
            fileScanner = new FileScanner();
        }
        return fileScanner;
    }

}
