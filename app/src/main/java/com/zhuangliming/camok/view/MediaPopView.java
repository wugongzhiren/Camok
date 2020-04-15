package com.zhuangliming.camok.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zhuangliming.camok.R;
import com.zhuangliming.camok.adapter.MediaListAdapter;
import com.zhuangliming.camok.model.MediaItem;
import com.zhuangliming.camok.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;


public class MediaPopView extends PopupWindow {
    Context mContext;
    private LayoutInflater mInflater;
    private View mContentView;
    private ImageView photo;
    private ImageView video;
    private LinearLayout container;
    private TextView goBackTx;
    private RecyclerView photoRv;
    private int width;
    private int height;
    private MediaListAdapter mediaListAdapter;
    private List<MediaItem> datas = new ArrayList<>();

    public MediaPopView(Context context) {
        super(context);
        this.mContext = context.getApplicationContext();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentView = mInflater.inflate(R.layout.pop_media, null);

        //设置View
        setContentView(mContentView);
        getScreenWH();
        //设置宽与高
        setWidth((int) (width*0.7));
        setHeight((int) (height*0.7));

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
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
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
        photo = mContentView.findViewById(R.id.photo);
        video = mContentView.findViewById(R.id.video);
        photoRv = mContentView.findViewById(R.id.mediaRv);
        container=mContentView.findViewById(R.id.container);
        goBackTx=mContentView.findViewById(R.id.backTv);
        mediaListAdapter = new MediaListAdapter(mContext, new ArrayList<>());
        photoRv.setLayoutManager(new LinearLayoutManager(mContext));
        photoRv.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
        photoRv.setAdapter(mediaListAdapter);
    }


    private void initListener() {
        goBackTx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(photoRv.getVisibility()==View.GONE){
                    return;
                }else{
                    container.setVisibility(View.VISIBLE);
                    photoRv.setVisibility(View.GONE);
                }
            }
        });
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //加载图片路径
                container.setVisibility(View.GONE);
                photoRv.setVisibility(View.VISIBLE);
                datas.clear();
                datas = FileUtil.getPhotoItems();
                mediaListAdapter.setDatas(datas);
            }
        });
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                container.setVisibility(View.GONE);
                photoRv.setVisibility(View.VISIBLE);
                datas.clear();
                datas = FileUtil.getVideoItems();
                mediaListAdapter.setDatas(FileUtil.getVideoItems());
            }
        });
        mediaListAdapter.setOnItemClickLitener(new MediaListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
               // Message message = Message.obtain();
                if (datas.get(position).type == 0) {
                    //Toast.makeText(mContext, "点击了：" + datas.get(position).name, Toast.LENGTH_SHORT).show();
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.SHOW_PHOTO,datas.get(position).url));
                }
                if (datas.get(position).type == 1) {
                   // Toast.makeText(mContext, "点击了：" + datas.get(position).name, Toast.LENGTH_SHORT).show();
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.PREVIEW_VIDEO,datas.get(position).url));
                }
               // mHandle.sendMessage(message);

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

