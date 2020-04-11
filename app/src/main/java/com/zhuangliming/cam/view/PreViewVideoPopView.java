package com.zhuangliming.cam.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.github.chrisbanes.photoview.PhotoView;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.widget.PLVideoView;
import com.zhuangliming.cam.MediaController;
import com.zhuangliming.cam.R;


public class PreViewVideoPopView extends PopupWindow {
    Context mContext;
    private LayoutInflater mInflater;
    private View mContentView;
    private PLVideoView videoView;
    private String videoPath;
    MediaController mMediaController;
    public PreViewVideoPopView(Context context, String videoPath) {
        super(context);
        this.mContext=context;
        this.videoPath=videoPath;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.activity_pl_video_view,null);

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

        videoView = mContentView.findViewById(R.id.VideoView);
        AVOptions options = new AVOptions();
        // the unit of timeout is ms
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        // 1 -> hw codec enable, 0 -> disable [recommended]
        options.setInteger(AVOptions.KEY_MEDIACODEC, AVOptions.MEDIA_CODEC_SW_DECODE);
        videoView.setVideoPath(videoPath);
        videoView.setLooping(true);

        // You can also use a custom `MediaController` widget
        mMediaController = new MediaController(mContext, true, false);
        //mMediaController.setOnClickSpeedAdjustListener(mOnClickSpeedAdjustListener);
        videoView.setMediaController(mMediaController);
        videoView.setAVOptions(options);
        videoView.start();
        //photo.setImageURI(Uri.parse(url));
    }


    private void initListener() {
        /*photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //加载图片路径
               dismiss();
            }
        });*/
    }
}
