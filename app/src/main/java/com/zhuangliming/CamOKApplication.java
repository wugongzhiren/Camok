package com.zhuangliming;

import android.app.Application;
import android.graphics.Bitmap;

public class CamOKApplication extends Application {


    private Bitmap mScreenCaptureBitmap;
    public Bitmap getmScreenCaptureBitmap() {
        return mScreenCaptureBitmap;
    }

    public void setmScreenCaptureBitmap(Bitmap mScreenCaptureBitmap) {
        this.mScreenCaptureBitmap = mScreenCaptureBitmap;
    }
}
