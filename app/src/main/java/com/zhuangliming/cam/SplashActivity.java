package com.zhuangliming.cam;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import static androidx.constraintlayout.widget.Constraints.TAG;

/**
 * 初始化
 */
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        checkPermission();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(SplashActivity.this,"启动",Toast.LENGTH_SHORT).show();
                Intent intent=new Intent(SplashActivity.this,MainActivity.class);
                SplashActivity.this.startActivity(intent);
                SplashActivity.this.finish();
            }
        },3000);
    }

    /**
     * 安卓6.0以上动态申请权限
     */
    private void checkPermission() {

        //Android平台版本，如我的版本为Android 7.1.2
        Log.v(TAG,"Build.VERSION.RELEASE----->"+ Build.VERSION.RELEASE);
        //当前手机版本-API版本号
        Log.v(TAG,"android.os.Build.VERSION.SDK_INT----->"+Build.VERSION.SDK_INT);
        //android 6.0 对应的 API版本号23
        Log.v(TAG,"Build.VERSION_CODES.M----->"+Build.VERSION_CODES.M);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//android 6.0以上
            Log.v(TAG,"测试手机版本为：android 6.0以上");

            int writePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);

            }else{
                Log.v(TAG,"测试手机版本为：android 6.0以上--->已申请");
            }
        }else{//android 6.0以下
            Log.v(TAG,"测试手机版本为：android 6.0以下");

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {//允许

                Log.v(TAG,"测试手机版本为：android 6.0以上--->未申请--->申请读写权限--->成功！");

            } else {//拒绝

                //Log.v(TAG,"测试手机版本为：android 6.0以上--->未申请--->申请读写权限--->失败！");
                Toast.makeText(this, "请赋予读写权限，否则应用部分功能将无法使用！", Toast.LENGTH_LONG).show();
                SplashActivity.this.finish();
            }
        }
    }

}
