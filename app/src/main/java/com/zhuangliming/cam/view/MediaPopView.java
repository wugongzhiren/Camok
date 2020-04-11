package com.zhuangliming.cam.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zhuangliming.cam.OsdSharePreference;
import com.zhuangliming.cam.R;
import com.zhuangliming.cam.adapter.PhotoListAdapter;

import java.util.ArrayList;


public class MediaPopView extends PopupWindow {
    Context mContext;
    private LayoutInflater mInflater;
    private View mContentView;
    private ImageView photo;
    private ImageView video;
    private RecyclerView photoRv;
    private Handler mHandle;
    private PhotoListAdapter photoListAdapter;
    public MediaPopView(Context context) {
        super(context);
        this.mContext=context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.pop_media,null);

        //设置View
        setContentView(mContentView);

        //设置宽与高
        setWidth(1200);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

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
        photo=mContentView.findViewById(R.id.photo);
        video=mContentView.findViewById(R.id.video);
        photoRv=mContentView.findViewById(R.id.mediaRv);
        photoListAdapter=new PhotoListAdapter(mContext,new ArrayList<>());
        photoRv.setLayoutManager(new LinearLayoutManager(mContext));
        photoRv.setAdapter(photoListAdapter);
    }


    private void initListener() {
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //加载图片路径
                FileUtil.getScreenShotsName()
            }
        });
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dismiss();
            }
        });
    }
}
