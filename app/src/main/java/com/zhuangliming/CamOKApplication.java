package com.zhuangliming;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;

import com.tencent.bugly.crashreport.CrashReport;

public class CamOKApplication extends Application {
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "5eb1ea6b26", false);
    }

    private Bitmap mScreenCaptureBitmap;
    public Bitmap getmScreenCaptureBitmap() {
        return mScreenCaptureBitmap;
    }

    public void setmScreenCaptureBitmap(Bitmap mScreenCaptureBitmap) {
        this.mScreenCaptureBitmap = mScreenCaptureBitmap;
    }

}
