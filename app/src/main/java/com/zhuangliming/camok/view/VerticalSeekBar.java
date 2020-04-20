package com.zhuangliming.camok.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.zhuangliming.camok.R;

public class VerticalSeekBar extends RelativeLayout implements View.OnTouchListener {

    public VerticalSeekBar(Context context) {
        super(context);
        init(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public interface Callback {
        public void changed(int mProgress);
    }

    private Callback mCallback;

    public void setCallback(Callback c) {
        mCallback = c;
    }

    private ImageView backgroundView;
    private ImageView mthumb;
    private ImageView progressView;

    private void init(Context context) {
        backgroundView = new ImageView(context);
        this.addView(backgroundView);
        progressView = new ImageView(context);
        this.addView(progressView);
        mthumb = new ImageView(context);
        this.addView(mthumb);
        setResource(this.getResources().getDrawable(R.drawable.seek_bg), this.getResources()
                        .getDrawable(R.drawable.progress_bg),
                this.getResources().getDrawable(R.drawable.thumb));
        this.setOnTouchListener(this);
        // mthumb.setOnTouchListener(this);
    }

    private void setResource(Drawable thumb, Drawable background, Drawable progress) {
        backgroundView.setImageDrawable(thumb);
        progressView.setImageDrawable(background);
        mthumb.setImageDrawable(progress);
    }

    private int offsetThumb = 18;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int width = this.getWidth();
        int height = this.getHeight();

        step = (height - 36) / (mMax);
        int backgroundwidt = backgroundView.getWidth();
        backgroundView.layout((width - backgroundwidt) / 2, offsetThumb, (width - backgroundwidt) / 2 + backgroundwidt,
                height - offsetThumb);
        int progressViewwidth = progressView.getWidth();


        progressView.layout((width - progressViewwidth) / 2, offsetThumb, (width - progressViewwidth) / 2
                + progressViewwidth, height - offsetThumb);
        mthumb.layout(0, 0, mthumb.getWidth(), mthumb.getHeight());

        setProgress(progress);
    }

    public void setProgress(int p) {
        if (p > mMax || p < 0) {
            return;
        }
        progress = p;
        int currentY = (int) (step * p);
        lastY = currentY;
        int y = 230 - 18 - currentY;
//		if (y > 18 && y < step) {
//			y = 18;
//			progress = (int) mMax;
//		}
        mthumb.layout(0, y - 18, mthumb.getWidth(), y + 18);
        int width = VerticalSeekBar.this.getWidth();
        int height = VerticalSeekBar.this.getHeight();
        int progressViewwidth = progressView.getWidth();
        progressView.layout((width - progressViewwidth) / 2, offsetThumb + y - 18, (width - progressViewwidth) / 2
                + progressViewwidth, height - offsetThumb);
        VerticalSeekBar.this.invalidate();
    }
    private float mMax;
    private float step;
    private int lastY;
    private int progress;
    public void setMax(int max) {
        mMax = max;
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int y = msg.arg1;
            if (y - 18 < 0) {
                y = 18;
            }
            if (y > VerticalSeekBar.this.getHeight() - 18) {
                y = VerticalSeekBar.this.getHeight() - 18;
            }

            if (y == VerticalSeekBar.this.getHeight() - 18) {
                progress = 0;
            } else if (y == 18||(y > 18 && y < step+18)) {
                progress = (int) mMax;
            } else {
                int tempY = Math.abs(y-212);



                //int tempY = Math.abs(y - (230 - 18));
                int number = (int) (tempY / step);
                progress = number;
                int currentY = (int) (step * number);
                if (currentY == lastY) {
                    return;
                }
                lastY = currentY;
                y = 230 - 18 - currentY;
//
//				if (y > 18 && y < step) {
//					y = 18;
//					progress = (int) mMax;
//				}
            }
            mthumb.layout(0, y - 18, mthumb.getWidth(), y + 18);
            int width = VerticalSeekBar.this.getWidth();
            int height = VerticalSeekBar.this.getHeight();
            int progressViewwidth = progressView.getWidth();
            progressView.layout((width - progressViewwidth) / 2, offsetThumb + y - 18, (width - progressViewwidth) / 2
                    + progressViewwidth, height - offsetThumb);
            VerticalSeekBar.this.invalidate();
            if (mCallback != null) {
                mCallback.changed(progress);
            }
        }
    };
    private boolean isTouchDown = false;
    public boolean isTouchDown() {
        return isTouchDown;
    }
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int y = 0;
        switch (event.getAction())// 根据动作来执行代码
        {
            case MotionEvent.ACTION_MOVE:// 滑动
                isTouchDown = true;
                y = (int) event.getY();
                handler.sendMessage(handler.obtainMessage(0, y, 0));
                break;
            case MotionEvent.ACTION_DOWN:// 按下
                isTouchDown = true;
                y = (int) event.getY();
                handler.sendMessage(handler.obtainMessage(0, y, 0));
                break;
            case MotionEvent.ACTION_UP:// 松开
            case MotionEvent.ACTION_CANCEL:// 松开
                isTouchDown = false;
                y = (int) event.getY();
                handler.sendMessage(handler.obtainMessage(0, y, 0));
                break;
            default:
        }
        return true;
    }
}