package com.zhuangliming.camok;

import android.content.Context;
import android.content.SharedPreferences;

public class OsdSharePreference {
    private SharedPreferences share;
    private SharedPreferences.Editor editor;
    private String SHARED_NAME = "osdInfo";//sp的文件名 自定义
    
    private OsdSharePreference(Context context) {
        share = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
        editor = share.edit();
    }

    /**
     * 单例模式
     */
    private static OsdSharePreference instance;//单例模式 双重检查锁定
    public static OsdSharePreference getInstance(Context context) {
        if (instance == null) {
            synchronized (OsdSharePreference.class) {
                if (instance == null) {
                    instance = new OsdSharePreference(context);
                }
            }
        }
        return instance;
    }
    /**
     * ------- Int ---------
     */
    public void putInt(String spName, int value) {
        editor.putInt(spName, value);
        editor.commit();
    }

    public int getInt(String spName, int defaultvalue) {
        return share.getInt(spName, defaultvalue);
    }

    /**
     * ------- String ---------
     */
    public void putString(String spName, String value) {
        editor.putString(spName, value);
        editor.commit();
    }

    public String getString(String spName, String defaultvalue) {
        return share.getString(spName, defaultvalue);
    }

    public String getString(String spName) {
        return share.getString(spName, "");
    }

}
