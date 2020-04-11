package com.zhuangliming.camok.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.zhuangliming.camok.OsdSharePreference;
import com.zhuangliming.camok.R;

import static android.content.Context.WINDOW_SERVICE;


public class OsdPopView extends PopupWindow {
    Context mContext;
    private LayoutInflater mInflater;
    private View mContentView;
    private TextView cancle;
    private TextView confirm;
    private EditText taskNameEdit;
    private EditText wellNameEdit;
    private EditText checkInfoEdit;
    private EditText checkCompanyEdit;
    private int width;
    private int height;
    private Handler mHandle;
    public OsdPopView(Context context, Handler handler) {
        super(context);
        this.mContext=context;
        this.mHandle=handler;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.layout_dialog,null);

        //设置View
        setContentView(mContentView);
        getScreenWH();
        //设置宽与高
        setWidth((int) (width*0.7));
        setHeight((int) (height*0.7));
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
        cancle=mContentView.findViewById(R.id.cancel);
        confirm=mContentView.findViewById(R.id.confirm);
        taskNameEdit=mContentView.findViewById(R.id.taskName);
        wellNameEdit=mContentView.findViewById(R.id.wellName);
        checkInfoEdit=mContentView.findViewById(R.id.checkInfo);
        checkCompanyEdit=mContentView.findViewById(R.id.checkCompany);
    }

    private void initListener() {
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //保存到本地
                OsdSharePreference.getInstance(mContext).putString("taskname",taskNameEdit.getText().toString());
                OsdSharePreference.getInstance(mContext).putString("wellname",wellNameEdit.getText().toString());
                OsdSharePreference.getInstance(mContext).putString("checkinfo",checkInfoEdit.getText().toString());
                OsdSharePreference.getInstance(mContext).putString("checkcompany",checkCompanyEdit.getText().toString());
                mHandle.sendEmptyMessage(0);
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
