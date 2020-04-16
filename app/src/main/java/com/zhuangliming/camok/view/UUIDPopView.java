package com.zhuangliming.camok.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.zhuangliming.camok.OsdSharePreference;
import com.zhuangliming.camok.R;
import com.zhuangliming.camok.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import static android.content.Context.WINDOW_SERVICE;


public class UUIDPopView extends PopupWindow {
    Context mContext;
    private LayoutInflater mInflater;
    private View mContentView;
    private TextView cancle;
    private TextView confirm;
    private EditText uuid;
    private EditText username;
    private EditText password;
    private int width;
    private int height;
    public UUIDPopView(Context context) {
        super(context);
        this.mContext=context.getApplicationContext();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.layout_uuid_pop,null);
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
        confirm=mContentView.findViewById(R.id.change);
        uuid=mContentView.findViewById(R.id.uuid);
        username=mContentView.findViewById(R.id.username);
        password=mContentView.findViewById(R.id.password);
        getUserInfo();
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
                //Log.i("UUID",username.getText().toString()==null?"1":"0");
                /*if(username.getText().toString().isEmpty()||password.getText().toString().isEmpty()||uuid.getText().toString().isEmpty()){
                    Toast.makeText(mContext,"请输入有效信息",Toast.LENGTH_SHORT).show();
                }else{*/
                    saveUserInfo();
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.DEVICE_CHANGE,null));
                    dismiss();
                //}
                //EventBus.getDefault().post(new MessageEvent(MessageEvent.SHOW_OSD,null));
                //dismiss();
            }
        });
    }


    private void getUserInfo() {
        uuid.setText(OsdSharePreference.getInstance(mContext).getString("uid"));
        username.setText(OsdSharePreference.getInstance(mContext).getString("username"));
        password.setText(OsdSharePreference.getInstance(mContext).getString("password"));
        /*SharedPreferences userInfo = getSharedPreferences(UID, MODE_PRIVATE);
        System.out.print("getUserInfo\n");
        String name;
        name = userInfo.getString("name", null);
        System.out.println(name);*/
    }
    /**
     * 保存参数
     */
    private void saveUserInfo() {
        OsdSharePreference.getInstance(mContext).putString("uid",uuid.getText().toString());
        OsdSharePreference.getInstance(mContext).putString("username",username.getText().toString());
        OsdSharePreference.getInstance(mContext).putString("password",password.getText().toString());
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
