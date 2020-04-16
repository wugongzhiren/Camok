package com.decode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.camera.model.api.AVAPIsClient;
import com.decode.tools.AvcUtils;
import com.jiangdg.natives.YuvUtils;

/**
 * 对YUV视频流进行编码
 * Created by jiangdongguo on 2017/5/6.
 */

public class EncoderVideoRunnable implements Runnable {
    private static final String TAG = "EncoderVideoRunnable";
    private byte[] cur_sps;
    private byte[] sps = {0, 0, 0, 1, 103, 77, 0, 30, -107, -88, 40, 11, -2, 89, -72, 8, 8, 8, 16};
    private byte[] sps_hd = {0, 0, 0, 1, 103, 77, 0, 31, -107, -88, 20, 1, 110, -101, -128, -128, -128, -127};
    private byte[] pps = {0, 0, 0, 1, 104, -18, 60, -128/*,0,0,0,1,6,-27,1,91,-128*/};
    private static final String MIME_TYPE = "video/avc";
    // 帧率
    private static final int FRAME_RATE = 20;
    // 间隔1s插入一帧关键帧
    private static final int FRAME_INTERVAL = 1;
    // 绑定编码器缓存区超时时间为10s
    private static final int TIMES_OUT = 10000;
    // 码率
    //private static final int BIT_RATE = CameraUtils.PREVIEW_WIDTH * CameraUtils.PREVIEW_HEIGHT * 3 * 8 * FRAME_RATE / 256;
    // 默认水平
    private boolean isPhoneHorizontal = true;

    // MP4混合器
    private WeakReference<MediaMuxerUtils> muxerRunnableRf;
    // 硬编码器
    private MediaCodec mVideoEncodec;
    private int mColorFormat;
    private boolean isExit = false;
    private boolean isEncoderStart = false;
    private boolean isAddTimeOsd = true;

    private BlockingQueue<byte[]> frameBytes;
    //    private byte[] mFrameData;
    //private boolean isFrontCamera;
    private long prevPresentationTimes;
    private MediaFormat mFormat;
    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test1.MP4";
    private BufferedOutputStream outputStream;
    private boolean isAddKeyFrame = false;
    int[] width = new int[1];
    int[] height = new int[1];
    private Surface mSurface;
    public EncoderVideoRunnable(WeakReference<MediaMuxerUtils> muxerRunnableRf,Surface surface) {
        this.muxerRunnableRf = muxerRunnableRf;
        this.mSurface=surface;
        frameBytes = new ArrayBlockingQueue<byte[]>(5);
        Log.i(TAG,"获取视频质量");
        byte quanlity = AVAPIsClient.nowQuality;
        System.out.println("视频质量:" + quanlity);
        if (quanlity == AVAPIsClient.AVIOCTRL_QUALITY_MIN
                || quanlity == AVAPIsClient.AVIOCTRL_QUALITY_LOW
                || quanlity == AVAPIsClient.AVIOCTRL_QUALITY_MIDDLE
                || quanlity == -1) {
            cur_sps = sps;
        } else  {
            cur_sps = sps_hd;
        }
//        mFrameData = new byte[CameraUtils.PREVIEW_WIDTH * CameraUtils.PREVIEW_HEIGHT *3 /2];
        initMediaFormat();
    }

    private void initMediaFormat() {
        try {
            mVideoEncodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            Log.e(TAG, "创建编码器失败" + e.getMessage());
            e.printStackTrace();
        }

        AvcUtils.parseSPS(this.cur_sps, width, height);//从sps中解析出视频宽高
        mFormat= MediaFormat.createVideoFormat("video/avc", width[0], height[0]);
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE, width[0]*height[0]);
        mFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        mFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mFormat.setByteBuffer("csd-0", ByteBuffer.wrap(this.sps));
        mFormat.setByteBuffer("csd-1", ByteBuffer.wrap(this.pps));
    }

    private void startCodec() {
        isExit = false;
        frameBytes.clear();
        if (mVideoEncodec != null) {
            mVideoEncodec.configure(mFormat, mSurface, null, 0);
            mVideoEncodec.start();
            isEncoderStart = true;
            Log.d(TAG, "配置、启动视频编码器");
        }
        //创建保存编码后数据的文件
        createfile();
    }

    private void stopCodec() {
        if (mVideoEncodec != null) {
            mVideoEncodec.stop();
            mVideoEncodec.release();
            mVideoEncodec = null;
            isAddKeyFrame = false;
            isEncoderStart = false;
            Log.d(TAG, "关闭视频编码器");
        }
        frameBytes.clear();
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addData(byte[] bufferInfo) {
        if (frameBytes != null) {
            // 这里不能用put，会一直不断地向阻塞线程写入数据
            //导致线程无法退出
            frameBytes.offer(bufferInfo);
        }
    }

    @Override
    public void run() {
        if (!isEncoderStart) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            startCodec();
        }
        // 如果编码器没有启动或者没有图像数据，线程阻塞等待
        while (!isExit) {
            try {
                byte[] bytes = frameBytes.take();
                encoderBytes(bytes);
            } catch (IllegalStateException e) {
                // 捕获因中断线程并停止混合dequeueOutputBuffer报的状态异常
                e.printStackTrace();
            } catch (NullPointerException e) {
                // 捕获因中断线程并停止混合MediaCodec为NULL异常
                e.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        stopCodec();
    }

    @SuppressLint({"NewApi", "WrongConstant"})
    private void encoderBytes(byte[] rawFrame) {
        ByteBuffer[] inputBuffers = mVideoEncodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mVideoEncodec.getOutputBuffers();
//		if(isFrontCamera()){
        // 前置旋转270度(即竖屏采集，此时isPhoneHorizontal=false)
//			YuvUtils.Yuv420spRotateOfFront(rawFrame, rotateNv21, mWidth, mHeight, 270);
//		}else{
        // 后置旋转90度(即竖直采集，此时isPhoneHorizontal=false)
//            YuvUtils.YUV420spRotateOfBack(rawFrame, rotateNv21, mWidth, mHeight, 90);
        // 后置旋转270度(即倒立采集，此时isPhoneHorizontal=false)
//			YuvUtils.YUV420spRotateOfBack(rawFrame, rotateNv21, mWidth, mHeight, 270);
        // 后置旋转180度(即反向横屏采集，此时isPhoneHorizontal=true)
//			YuvUtils.YUV420spRotateOfBack(rawFrame, rotateNv21, mWidth, mHeight, 180);
        // 如果是正向横屏，则无需旋转YUV，此时isPhoneHorizontal=true
//		}
        // 将NV21转换为编码器支持的颜色格式I420，添加时间水印
//		if(isAddTimeOsd){
//            YuvUtils.AddYuvOsd(rotateNv21, mWidth, mHeight, mFrameData,
//					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
//					mColorFormat,isPhoneHorizontal);
//		}else{
//            YuvUtils.transferColorFormat(rotateNv21, mWidth, mHeight, mFrameData, mColorFormat);
//		}
        // 将NV21转换为YUV420p
/*        if(mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
            YuvUtils.nativeNV21ToYUV420p(rawFrame, mWidth, mHeight);
        } else {
            YuvUtils.nativeNV21ToYUV420sp(rawFrame,mWidth,mHeight);
        }*/

        // 叠加时间水印
       // YuvUtils.addYuvOsd(rawFrame, width[0], height[0], true,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), 100, 100);

        //返回编码器的一个输入缓存区句柄，-1表示当前没有可用的输入缓存区
        int inputBufferIndex = mVideoEncodec.dequeueInputBuffer(TIMES_OUT);
        if (inputBufferIndex >= 0) {
            // 绑定一个被空的、可写的输入缓存区inputBuffer到客户端
            ByteBuffer inputBuffer = null;
            if (!isLollipop()) {
                inputBuffer = inputBuffers[inputBufferIndex];
            } else {
                inputBuffer = mVideoEncodec.getInputBuffer(inputBufferIndex);
            }
            // 向输入缓存区写入有效原始数据，并提交到编码器中进行编码处理
            inputBuffer.clear();
            inputBuffer.put(rawFrame);
            mVideoEncodec.queueInputBuffer(inputBufferIndex, 0, rawFrame.length, getPTSUs(), 0);
        }

        // 返回一个输出缓存区句柄，当为-1时表示当前没有可用的输出缓存区
        // mBufferInfo参数包含被编码好的数据，timesOut参数为超时等待的时间
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = -1;
        do {
            outputBufferIndex = mVideoEncodec.dequeueOutputBuffer(mBufferInfo, TIMES_OUT);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.i(TAG, "获得编码器输出缓存区超时");
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // 如果API小于21，APP需要重新绑定编码器的输入缓存区；
                // 如果API大于21，则无需处理INFO_OUTPUT_BUFFERS_CHANGED
                if (!isLollipop()) {
                    outputBuffers = mVideoEncodec.getOutputBuffers();
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 编码器输出缓存区格式改变，通常在存储数据之前且只会改变一次
                // 这里设置混合器视频轨道，如果音频已经添加则启动混合器（保证音视频同步）
               /* MediaFormat newFormat = mVideoEncodec.getOutputFormat();
                MediaMuxerUtils mMuxerUtils = muxerRunnableRf.get();
                if (mMuxerUtils != null) {
                    mMuxerUtils.setMediaFormat(MediaMuxerUtils.TRACK_VIDEO, newFormat);
                }*/
                Log.i(TAG, "编码器输出缓存区格式改变，添加视频轨道到混合器");
            } else {
                // 获取一个只读的输出缓存区inputBuffer ，它包含被编码好的数据
                ByteBuffer outputBuffer = null;
                if (!isLollipop()) {
                    outputBuffer = outputBuffers[outputBufferIndex];
                } else {
                    outputBuffer = mVideoEncodec.getOutputBuffer(outputBufferIndex);
                }
                // 如果API<=19，需要根据BufferInfo的offset偏移量调整ByteBuffer的位置
                // 并且限定将要读取缓存区数据的长度，否则输出数据会混乱
                if (isKITKAT()) {
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                }
                // 根据NALU类型判断帧类型
               // MediaMuxerUtils mMuxerUtils = muxerRunnableRf.get();
                int type = outputBuffer.get(4) & 0x1F;
                Log.d(TAG, "------还有数据---->" + type);
                if (type == 7 || type == 8) {
                    Log.e(TAG, "------PPS、SPS帧(非图像数据)，忽略-------");
                    mBufferInfo.size = 0;
                } else if (type == 5) {
                    // 录像时，第1秒画面会静止，这是由于音视轨没有完全被添加
                    // Muxer没有启动
                    Log.e(TAG, "------I帧(关键帧)-------");
                   /* mMuxerUtils.addMuxerData(new MediaMuxerUtils.MuxerData(
                            MediaMuxerUtils.TRACK_VIDEO, outputBuffer,
                            mBufferInfo));*/
                    prevPresentationTimes = mBufferInfo.presentationTimeUs;
                    isAddKeyFrame = true;
                   /* if (mMuxerUtils != null && mMuxerUtils.isMuxerStarted()) {
                        mMuxerUtils.addMuxerData(new MediaMuxerUtils.MuxerData(
                                MediaMuxerUtils.TRACK_VIDEO, outputBuffer,
                                mBufferInfo));
                        prevPresentationTimes = mBufferInfo.presentationTimeUs;
                        isAddKeyFrame = true;
                        Log.e(TAG, "----------->添加关键帧到混合器");
                    }*/
                } else {
                    if (isAddKeyFrame) {
                        prevPresentationTimes = mBufferInfo.presentationTimeUs;
                        Log.d(TAG, "------非I帧(type=1)，添加到混合器-------");
                       /* if (mMuxerUtils != null && mMuxerUtils.isMuxerStarted()) {
                            mMuxerUtils.addMuxerData(new MediaMuxerUtils.MuxerData(
                                    MediaMuxerUtils.TRACK_VIDEO, outputBuffer,
                                    mBufferInfo));
                            prevPresentationTimes = mBufferInfo.presentationTimeUs;
                            Log.d(TAG, "------添加到混合器");
                        }*/
                    }
                }
                // 处理结束，释放输出缓存区资源
                mVideoEncodec.releaseOutputBuffer(outputBufferIndex, true);
            }
        } while (outputBufferIndex >= 0);
    }

    public void exit() {
        isExit = true;
    }

    /**
     * 遍历所有编解码器，返回第一个与指定MIME类型匹配的编码器
     * 判断是否有支持指定mime类型的编码器
     */
    private MediaCodecInfo selectSupportCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            // 判断是否为编码器，否则直接进入下一次循环
            if (!codecInfo.isEncoder()) {
                continue;
            }
            // 如果是编码器，判断是否支持Mime类型
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }


    /**
     * 根据mime类型匹配编码器支持的颜色格式
     */
    private int selectSupportColorFormat(MediaCodecInfo mCodecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = mCodecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isCodecRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        return 0;
    }

    private boolean isCodecRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
//            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                return true;
            default:
                return false;
        }
    }

    private boolean isLollipop() {
        // API>=21
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private boolean isKITKAT() {
        // API<=19
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT;
    }

    private long getPTSUs() {
        long result = System.nanoTime() / 1000;
        if (result < prevPresentationTimes) {
            result = (prevPresentationTimes - result) + result;
        }
        return result;
    }

    private void createfile() {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
