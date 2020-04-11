package com.zhuangliming.cam.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;
import com.zhuangliming.cam.R;
import com.zhuangliming.cam.adapter.MediaListAdapter;

import java.util.ArrayList;

import static android.content.Context.WINDOW_SERVICE;


public class PreViewPhotoPopView extends PopupWindow {
    Context mContext;
    private LayoutInflater mInflater;
    private View mContentView;
    private PhotoView photo;
    private String url;
    private int width;
    private int height;
    public PreViewPhotoPopView(Context context,String url) {
        super(context);
        this.mContext=context;
        this.url=url;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.pop_pre_photo,null);

        //设置View
        setContentView(mContentView);
        getScreenWH();
        //设置宽与高
        setWidth(width);
        setHeight(height);

        /**
         * 设置进出动画
         */
       // setAnimationStyle(R.style.MyPopupWindow);

        /**
         * 设置背景只有设置了这个才可以点击外边和BACK消失
         */
        setBackgroundDrawable(new ColorDrawable());

        /**
         * 设置可以获取集点
         */
        setFocusable(true);

        /**
         * 设置点击外边可以消失
         */
        setOutsideTouchable(true);

        /**
         *设置可以触摸
         */
        setTouchable(true);

        /**
         * 设置点击外部可以消失
         */

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                /**
                 * 判断是不是点击了外部
                 */
                if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
                    return true;
                }
                //不是点击外部
                return false;
            }
        });

        /**
         * 初始化View与监听器
         */
        initView();
        initListener();
    }

    private void initView() {
        photo=mContentView.findViewById(R.id.screenPhoto);
        photo.setImageURI(Uri.parse(url));
    }


    private void initListener() {
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //加载图片路径
               dismiss();
            }
        });
    }
    private void getScreenWH() {
        //context的方法，获取windowManager
        WindowManager windowManager = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
        //获取屏幕对象
        Display defaultDisplay = windowManager.getDefaultDisplay();
        //获取屏幕的宽、高，单位是像素
        width = defaultDisplay.getWidth();
        height = defaultDisplay.getHeight();
        Log.i("getScreenWH","width="+width+"height="+height);
    }
}
