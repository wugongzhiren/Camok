package com.decode;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.decode.tools.BufferInfo;

public class MediaMuxerUtils {
    private static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String TAG = "MediaMuxerUtils";
    public static final int TRACK_VIDEO = 0;
    private boolean isVideoAdded;
    public boolean isRecord = false;
    private boolean isMuxerStarted;
    public boolean isExit = false;
    private int videoTrack = -1;

    private Object lock = new Object();
    private Vector<MuxerData> mMuxerDatas;
    private MediaMuxer mMuxer;
    private MediaFormat videoMediaFormat;
    private EncoderVideoRunnable videoRunnable;
    private Thread mMuxerThread;
    private Thread mVideoThread;
    private static MediaMuxerUtils muxerUtils;
    private Surface mSurface;

    private MediaMuxerUtils(Surface surface) {
        this.mSurface = surface;
    }

    public static MediaMuxerUtils getMuxerRunnableInstance(Surface surface) {
        //if (muxerUtils == null) {
        muxerUtils = new MediaMuxerUtils(surface);
        return muxerUtils;
    }

    public void startDecode() {
        mMuxerDatas = new Vector<>();
        //启动解码线程，显示在界面
        videoRunnable = new EncoderVideoRunnable(new WeakReference<>(this), mSurface);
        mVideoThread = new Thread(videoRunnable);
        mVideoThread.start();
        //isExit = false;
    }

    //保存视频流线程
    class MediaMuxerRunnable implements Runnable {
        @Override
        public void run() {
            //initMuxer();
            while (!isExit) {
                Log.w(TAG, "录制--->循环开始");
                // 混合器没有启动或数据缓存为空，则阻塞混合线程等待启动(数据输入)
                if (isMuxerStarted) {
                    // 从缓存读取数据写入混合器中       
                    if (mMuxerDatas.isEmpty()) {
                        Log.w(TAG, "录制run--->混合器没有数据，阻塞线程等待");
                        synchronized (lock) {
                            try {
                                lock.wait();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        MuxerData data = mMuxerDatas.remove(0);
                        if (data != null) {
                            int track = 0;
                            try {
                                //if (data.trackIndex == TRACK_VIDEO) {
                                   // track = videoTrack;
                                    Log.d(TAG, "录制---写入视频数据---");
                                //}
                                mMuxer.writeSampleData(videoTrackIndex, data.byteBuf, data.bufferInfo);
                            } catch (Exception e) {
                                Log.e(TAG, "录制--写入数据到混合器失败，track=" + track);
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "run--->混合器没有启动，阻塞线程等待");
                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            stopMuxer();
        }
    }
    static int videoTrackIndex;
    public void startMuxer() {
        Log.e(TAG, "录制--启动混合器，mMuxer=null");
        try {
            String dirpath =
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/TUTK_VIDEOS";
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);
            File file = new File(dirpath);
            if (!file.exists()) {
                file.mkdirs();
            }
            String path = dirpath + "/" + df.format(new Date()) + ".mp4";
            mMuxer = new MediaMuxer(path,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex=mMuxer.addTrack(videoRunnable.mFormat);
            mMuxer.start();
            //启动混合器线程
            startMuxerThread();
            isMuxerStarted = true;
            synchronized (lock) {
                Log.d(TAG, "录制-----lock 锁---");
                lock.notify();
            }
            Log.d(TAG, "录制-----启动混合器---");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMuxer() {
        if (mMuxer == null) {
            Log.e(TAG, "停止混合器失败，mMuxer=null");
            return;
        }
        Log.d(TAG, "录制---停止混合器---");
        if (isMuxerStarted) {
            mMuxer.stop();
            mMuxer.release();
            videoMediaFormat = null;
            isVideoAdded = false;
            isMuxerStarted = false;
            mMuxer = null;
        }
    }

   /* // 添加音、视频轨道
    public void record(int index, MediaFormat meidaFormat) {
        Log.i(TAG, "录制--添加轨道，index=" + index);
        if (mMuxer == null && isMuxerStarted) {
            Log.e(TAG, "录制--添加轨道失败或混合器已经启动，index=" + index);
            return;
        }
        if (index == TRACK_VIDEO) {
            if (videoMediaFormat == null) {
                videoMediaFormat = meidaFormat;
                videoTrack = mMuxer.addTrack(videoMediaFormat);
                isVideoAdded = true;
                Log.d(TAG, "录制--添加视频轨道到混合器---");
            }
        }
        startMuxer();
    }*/

    // 向MediaMuxer添加数据
    public void addMuxerData(MuxerData data) {
        Log.i(TAG, "MediaMuxer添加数据");
        if (mMuxerDatas == null) {
            Log.e(TAG, "添加数据失败");
            return;
        }
        mMuxerDatas.add(data);
        // 解锁
        synchronized (lock) {
            lock.notify();
        }
    }

    // 添加图像数据到视频编码器
    public void addVideoFrameData(BufferInfo frameData) {
        if (videoRunnable != null) {
            videoRunnable.addData(frameData);
        }
    }

    public void startMuxerThread() {
        Log.d(TAG, "---启动混合器线程---");
        if (mMuxerThread == null) {
            synchronized (MediaMuxerUtils.this) {
                mMuxerThread = new Thread(new MediaMuxerRunnable());
                mMuxerThread.start();
            }
        }
    }

    public void stopMuxerThread() {
        exit();
        if (mMuxerThread != null) {
            try {
                mMuxerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mMuxerThread = null;
    }

    public void exit() {
        Log.d(TAG, "---停止混合器(录音、录像)线程---");
        // 清理视频录制线程资源
        videoRunnable.exit();
        this.mSurface.release();
        this.mSurface=null;
        if (videoRunnable != null) {
            videoRunnable.exit();
        }
        if (mVideoThread != null) {
            try {
                mVideoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mVideoThread = null;
        }
        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 切换设备时候除了surface不需要释放，其他都需要
     */
    public void releaseDecodec(){
        videoRunnable.stopCodec();
        stop();
    }
    /**
     * 断网时停止线程
     */
    public void stop(){
        Log.d(TAG, "---停止混合器(录音、录像)线程---");
        // 清理视频录制线程资源
        videoRunnable.exit();
        if (videoRunnable != null) {
            videoRunnable.exit();
        }
        if (mVideoThread != null) {
            mVideoThread.stop();
            mVideoThread = null;
        }
        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }
    public boolean isMuxerStarted() {
        return isMuxerStarted;
    }

    public boolean isVideoAdded() {
        return isVideoAdded;
    }


    private boolean isCanStartMuxer() {
        return isVideoAdded;
    }

    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }
}
