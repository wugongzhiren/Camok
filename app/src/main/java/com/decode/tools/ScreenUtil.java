package com.decode.tools;

import java.io.IOException;

public class ScreenUtil {
    public static boolean screenCap(){
        String filename=System.currentTimeMillis()+".png";
        try {
            Runtime.getRuntime().exec("su");
            Runtime.getRuntime().exec("adb shell /system/bin/screencap -p /data/data/"+filename);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
