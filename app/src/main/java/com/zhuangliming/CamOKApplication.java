package com.zhuangliming;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;

public class CamOKApplication extends Application {
    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    private Bitmap mScreenCaptureBitmap;
    public Bitmap getmScreenCaptureBitmap() {
        return mScreenCaptureBitmap;
    }

    public void setmScreenCaptureBitmap(Bitmap mScreenCaptureBitmap) {
        this.mScreenCaptureBitmap = mScreenCaptureBitmap;
    }

}
