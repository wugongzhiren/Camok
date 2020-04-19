package com.camera.camerawithtutk;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.util.Log;

import com.camera.model.SaveFrames;
import com.decode.tools.BufferInfo;
import com.tutk.IOTC.AVAPIs;
import com.zhuangliming.camok.R;

import java.util.concurrent.BlockingDeque;

/**
 * 用来接收视频数据的线程
 * 一帧一帧接收
 */
public class VideoThread implements Runnable {
    static final int VIDEO_BUF_SIZE = 100000; // 预计视频buf大小
    static final int FRAME_INFO_SIZE = 16;// 帧信息大小

    private int avIndex; // 需要传入的avIndex
    private BlockingDeque bq;
    public static boolean startReceive = false;
    public VideoThread(int avIndex, BlockingDeque bq) {
        this.avIndex = avIndex;
        //this.handler = handler;
        this.bq = bq;
        System.out.println("VideoThread");
    }

    @Override
    public void run() {
        // 响应中断
        if (Thread.currentThread().isInterrupted()) {
            System.out.println("Java技术栈线程被中断，程序退出。");
            return;
        }
        System.out.printf("[%s] 开始接收视频\n",
                Thread.currentThread().getName());
        AVAPIs av = new AVAPIs();
        byte[] frameInfo = new byte[FRAME_INFO_SIZE];
        int[] outBufSize = new int[1];
        int[] outFrameSize = new int[1];
        int[] outFrmInfoBufSize = new int[1];
        SaveFrames saveFrames = new SaveFrames();
        byte[] videoBuffer = new byte[VIDEO_BUF_SIZE];  // 用来存取视频帧
        int[] frameNumber = new int[1];
        while (true) {
            // 返回结果为接收视频videoBuffer的实际长度
            int ret = av.avRecvFrameData2(avIndex, videoBuffer,
                    VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                    frameInfo, FRAME_INFO_SIZE,
                  outFrmInfoBufSize, frameNumber);
            Log.i("videoBuffer","videoBuffer实际长度"+ret+"长度:"+videoBuffer.length);
              if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
                try {
                    Thread.sleep(30);
                    continue;
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                    break;
                }
            } else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                System.out.printf("[%s] Lost video frame number[%d]\n",
                        Thread.currentThread().getName(), frameNumber[0]);
                continue;
            } else if (ret == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                System.out.printf("[%s] Incomplete video frame number[%d]\n",
                        Thread.currentThread().getName(), frameNumber[0]);
                continue;
            } else if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                System.out.printf("[%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
                        Thread.currentThread().getName());
                break;
            } else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                System.out.printf("[%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
                        Thread.currentThread().getName());
                break;
            } else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                System.out.printf("[%s] Session cant be used anymore\n",
                        Thread.currentThread().getName());
                break;
            }
            // Now the data is ready in videoBuffer[0 ... ret - 1]
            //----------------------把videobuffer信息加入阻塞队列----------------------
            try {
                Log.i("videoBuffer","outFrameSize:"+outFrameSize[0]);
                BufferInfo bi = new BufferInfo(outFrameSize[0], videoBuffer,1);
                bq.offer(bi);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //---------------------------------------------------------------------
            if (startReceive) {
                saveFrames.saveFrames(videoBuffer, frameInfo, ret);
            } else {
                saveFrames.stopReceive();
            }

        }


       /* System.out.printf("[%s] 退出\n",
                Thread.currentThread().getName());*/
    }

    //缩小图片到制定长宽
    public static Bitmap scaleImage(Bitmap bm, int newWidth, int newHeight)
    {
        if (bm == null)
        {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        if (bm != null & !bm.isRecycled())
        {
            bm.recycle();
            bm = null;
        }
        return newbm;
    }

    //图片转YUV
    public byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;

        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] data = rgb2YCbCr420(pixels, width, height);

        return data;
    }

    public static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        //yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                //屏蔽ARGB的透明度值
                int rgb = pixels[i * width + j] & 0x00FFFFFF;
                //像素的颜色顺序为bgr，移位运算。
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                //套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

                y = y < 16 ? 16 : (y > 255 ? 255 : y);
                u = u < 0 ? 0 : (u > 255 ? 255 : u);
                v = v < 0 ? 0 : (v > 255 ? 255 : v);
                //赋值
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1) + 0] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }
}