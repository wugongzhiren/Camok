package com.zhuangliming.camok.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.decode.MediaCodecDecoder;

public abstract class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback  {
    SurfaceHolder surfaceHolder;
    MediaCodecDecoder mediaCodecDecoder;
    public MySurfaceView(Context context,MediaCodecDecoder mediaCodecDecoder) {
        super(context);
        surfaceHolder = this.getHolder();
        this.mediaCodecDecoder=mediaCodecDecoder;
        surfaceHolder.addCallback(this);
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaCodecDecoder.configure(getHolder().getSurface());
        System.out.println("配置解码器");

        // 启动解码器
        mediaCodecDecoder.start();
        System.out.println("启动解码器");
       // new Thread(new MyThread()).start();
        //new DrawThread(getHolder().getSurface()).start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mediaCodecDecoder.release();
    }
    //将绘制图案的方法抽象出来，让子类实现，调用getBitmap方法时就会调用此方法
    protected abstract void doDraw(Canvas canvas);

    private class MyThread extends Thread {

        @Override
        public void run() {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Canvas canvas = surfaceHolder.lockCanvas(null);//获取画布
            doDraw(canvas);
            if(canvas!=null){
                surfaceHolder.unlockCanvasAndPost(canvas);//解锁画布，提交画好的图像
            }


        }
    }

    //调用该方法将doDraw绘制的图案绘制在自己的canvas上
    public Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        doDraw(canvas);
        return bitmap;
    }
}
