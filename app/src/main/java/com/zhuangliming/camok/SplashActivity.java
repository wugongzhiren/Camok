package com.zhuangliming.camok;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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
        //talk();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //没有网络
                if(!isNetworkConnected()){
                    Toast.makeText(SplashActivity.this,"没有网络连接,即将退出！",Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            SplashActivity.this.finish();
                        }
                    },2000);

                }else {
                    //Toast.makeText(SplashActivity.this,"启动",Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(SplashActivity.this,MainActivity.class);
                    SplashActivity.this.startActivity(intent);
                    SplashActivity.this.finish();
                }

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
    public boolean isNetworkConnected() {
            ConnectivityManager mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                //这种方法也可以
                return mNetworkInfo .getState()== NetworkInfo.State.CONNECTED;
               // return mNetworkInfo.isAvailable();
        }
        return false;
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

    //本地一个TCP客户端
    public void talk() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket server = null;
                try {
                    server = new ServerSocket(8100);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("监控端口：" + 8100);
                Socket socket = null;
                while (true) {
                    try {
                        Log.i("socket接收","等待接收");
                        // 阻塞等待，每接收到一个请求就创建一个新的连接实例
                        socket = server.accept();
                        System.out.println("连接客户端地址：" + socket.getRemoteSocketAddress());

                        // 装饰流BufferedReader封装输入流（接收客户端的流）
                        BufferedInputStream bis = new BufferedInputStream(
                                socket.getInputStream());

                        DataInputStream dis = new DataInputStream(bis);
                        byte[] bytes = new byte[1024]; // 一次读取一个byte
                        String ret = "";
                        Log.i("socket接收","bytes长度");
                        while (dis.read(bytes) != -1) {
                            byte[] temp = new byte[64];
                            System.arraycopy(bytes,56,temp,0,8);
                            String text = new String(temp,"GBK");
                            Log.i("socket接收", "talk: "+text);
                            break;
                    /*ret += bytesToHexString(bytes) + " ";
                    if (dis.available() == 0) { //一个请求
                        doSomething(ret);
                    }*/
                        }

                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        }).start();


    }
}
