package com.zhuangliming.camok.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

import com.zhuangliming.camok.R;
import com.zhuangliming.camok.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;

public class WifiStateView  extends AppCompatImageView {


    public static final String TAG = "WifiStateView";
    private static final int LEVEL_DGREE = 4;
    //定义wifi信号等级
    private static final int LEVEL_0 = 0;
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;
    private static final int LEVEL_3 = 3;
    private static final int LEVEL_NONE = -1;

    WifiManager mWifiManager;
    WifiHandler mWifiHandler;
    ConnectivityManager mCM;

    //将handle设定为static，防止内存泄漏
    private static class WifiHandler extends Handler {
        WeakReference<WifiStateView> mView;
        public WifiHandler(WifiStateView view){
            mView = new WeakReference<WifiStateView>(view);

        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if(mView.get() == null){
                return;
            }
            WifiStateView view = mView.get();
            Log.i(TAG, "handleMessage level " + msg.what);
            //根据wifi的信号强度等级，更换图标
            switch (msg.what) {
                case LEVEL_0:
                    view.setImageResource(R.drawable.wifi4);
                    break;
                case LEVEL_1:
                    view.setImageResource(R.drawable.wifi3);
                    break;
                case LEVEL_2:
                    view. setImageResource(R.drawable.wifi2);
                    break;

                case LEVEL_3:
                    view. setImageResource(R.drawable.wifi1);
                    break;

                case LEVEL_NONE:
                    view.setImageResource(R.drawable.wifi6);
                    break;

                default:
                    view.setImageResource(R.drawable.wifi0);
                    break;
            }
        }

    }


    private BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //WifiManager.WIFI_STATE_CHANGED_ACTION
            Log.i(TAG, "onReceive "+action);
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                Log.i(TAG, "onReceive 处理"+action);
                //如果wifi没有连接成功，则显示wifi图标无连接的状态
                if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                    mWifiHandler.sendEmptyMessage(LEVEL_NONE);
                    return;
                }

            }else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                Log.i(TAG, "onReceive 处理"+action);
                //当前网络无连接，当前网络不是wifi连接,当前网络是wifi但是没有连接，wifi图标都显示无连接
                if(isNetworkConnected(context)){
                    Log.i(TAG,"网络连接");
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.NET_CONNECT,null));
                }else {
                    Log.i(TAG,"无网络连接");
                    mWifiHandler.sendEmptyMessage(LEVEL_NONE);
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.NET_LOSS,null));

                }

            }else if(action.equals(WifiManager.RSSI_CHANGED_ACTION)){
                //当信号的rssi值发生变化时，在这里处理
                Log.i(TAG, "onReceive 处理"+action);
                if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                    mWifiHandler.sendEmptyMessage(LEVEL_NONE);
                    return;
                }
                WifiInfo info = mWifiManager.getConnectionInfo();
                //计算wifi的信号等级
                int level = WifiManager.calculateSignalLevel(info.getRssi(), LEVEL_DGREE);
                Log.i(TAG,"wifi level "+level);
                mWifiHandler.sendEmptyMessage(level);
            }
        }
    };
    public boolean isNetworkConnected(Context context) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo != null) {
            //这种方法也可以
            return mNetworkInfo .getState()== NetworkInfo.State.CONNECTED;
            // return mNetworkInfo.isAvailable();
        }
        return false;
    }
    public WifiStateView(Context context) {
        this(context, null);
    }

    public WifiStateView(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public WifiStateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mCM = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiHandler = new WifiHandler(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter mFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        //注册广播接收器
        getContext().registerReceiver(mWifiStateReceiver, mFilter);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWifiHandler.removeCallbacksAndMessages(null);
        //注销广播接收器
        getContext().unregisterReceiver(mWifiStateReceiver);
    }


}
