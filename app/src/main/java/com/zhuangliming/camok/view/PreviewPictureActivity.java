package com.zhuangliming.camok.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.zhuangliming.CamOKApplication;
import com.zhuangliming.camok.R;

/**
 *
 */
public class PreviewPictureActivity extends FragmentActivity implements GlobalScreenshot.onScreenShotListener {

    public static final Intent newIntent(Context context) {
        Intent intent = new Intent(context, PreviewPictureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    private ImageView mPreviewImageView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preview_layout);
        mPreviewImageView = (ImageView) findViewById(R.id.preview_image);

        GlobalScreenshot screenshot = new GlobalScreenshot(getApplicationContext());

        Bitmap bitmap = ((CamOKApplication) getApplication()).getmScreenCaptureBitmap();


        Log.e("ryze", "预览图片");
        mPreviewImageView.setImageBitmap(bitmap);
        mPreviewImageView.setVisibility(View.GONE);

        if (bitmap != null) {
            screenshot.takeScreenshot(bitmap, this, true, true);
        }

    }

    @Override
    public void onStartShot() {

    }

    @Override
    public void onFinishShot(boolean success) {
        mPreviewImageView.setVisibility(View.VISIBLE);
    }
}
