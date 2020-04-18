package com.decode;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.camera.model.api.AVAPIsClient;
import com.decode.tools.AvcUtils;
import com.decode.tools.BufferInfo;
import com.zhuangliming.camok.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * @file VideoDecoder
 * @brief h264视频解码器
 * @date 2016/7/29
 */
public class VideoDecoder {
    private Surface mSurface;
    private static final int TYPE_SPS = 7;
    private static final int TYPE_PPS = 8;
    private static final int TYPE_FRAME_DATA = 5;
    private static final int NO_FRAME_DATA = -1;
    private final int TIMEOUT_US = 10000;
    private static final String TAG = "VideoDecoder";
    private byte[] cur_sps;
    private byte[] sps = {0, 0, 0, 1, 103, 77, 0, 30, -107, -88, 40, 11, -2, 89, -72, 8, 8, 8, 16};
    private byte[] sps_hd = {0, 0, 0, 1, 103, 77, 0, 31, -107, -88, 20, 1, 110, -101, -128, -128, -128, -127};
    private byte[] pps = {0, 0, 0, 1, 104, -18, 60, -128/*,0,0,0,1,6,-27,1,91,-128*/};
    //private Server mServer;
    //private AVAPIsClient client;
    private Worker mWorker;
    private static MediaCodec decoder;
    volatile boolean isRunning;
    private MediaFormat format;
    public static MediaMuxer mediaMuxer = null;
    public static boolean isNeedRecord=false;

    public VideoDecoder(Surface surface) {
        mSurface = surface;
        //mServer=server;
       // this.client=client;
        //获取服务器
        Log.i(TAG,"获取视频质量");
        byte quanlity = AVAPIsClient.nowQuality;
        System.out.println("视频质量:" + quanlity);
        if (quanlity == AVAPIsClient.AVIOCTRL_QUALITY_MIN
                || quanlity == AVAPIsClient.AVIOCTRL_QUALITY_LOW
                || quanlity == AVAPIsClient.AVIOCTRL_QUALITY_MIDDLE
                || quanlity == -1) {
            cur_sps = sps;
        } else if (quanlity == AVAPIsClient.AVIOCTRL_QUALITY_HIGH) {
            cur_sps = sps_hd;
        } else {
            return;
        }
    }
    private static MediaCodec.BufferInfo videoTrackInfo = new MediaCodec.BufferInfo();
    int frameRate;
    static int videoTrackIndex;
    String path;
    public boolean startRecord(){
        Log.i("Muxer", "开始录制准备");
        if(format==null){
            //首先读取编码的视频的长度和宽度
            int[] width = new int[1];
            int[] height = new int[1];
           /* mSps=Arrays.copyOfRange(spspps,0,pos);
            mPps= Arrays.copyOfRange(spspps,pos,spspps.length);*/
            AvcUtils.parseSPS(this.cur_sps, width, height);//从sps中解析出视频宽高
            format= MediaFormat.createVideoFormat("video/avc", width[0], height[0]);
            Log.i(TAG, "width:" + width[0] + "; height:" + height[0]);
            //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width[0], height[0] * mWidth);
            //format.setInteger(MediaFormat.KEY_MAX_HEIGHT, mHeight);
            //format.setInteger(MediaFormat.KEY_MAX_WIDTH, mWidth);
            format.setInteger(MediaFormat.KEY_BIT_RATE, width[0]*height[0]);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setByteBuffer("csd-0", ByteBuffer.wrap(this.cur_sps));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(this.pps));
        }
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        String dirpath =
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/TUTK_VIDEOS";
        SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);
        File file=new File(dirpath);
        if(!file.exists()){
            file.mkdirs();
        }
        path = dirpath + "/" + df.format(new Date()) + ".mp4";
        try {
            mediaMuxer=new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //            帧率
            frameRate= format.getInteger(MediaFormat.KEY_FRAME_RATE);
            Log.i("Muxer",frameRate+"");
            videoTrackIndex= mediaMuxer.addTrack(format);
            videoTrackInfo.presentationTimeUs = 0;
            this.isNeedRecord=true;
            mediaMuxer.start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    public void stopRecord(){
        Log.i("Muxer","停止录制");
        this.isNeedRecord=false;
        mediaMuxer.stop();
        mediaMuxer.release();
        EventBus.getDefault().post(new MessageEvent(MessageEvent.RECORD_COMPLETE,path));
    }
    /**
     * 等待客户端连接，解码器配置
     *
     * @return
     */
    public boolean config(Context context) {

        Log.i(TAG,"config");
        if(!AVAPIsClient.isStarted){
            new Thread(new Runnable() {
                @Override
                public void run() {
                   // AVAPIsClient.start(context);
                }
            }).start();
        }
        //mBufferInfo = new MediaCodec.BufferInfo();
        //首先读取编码的视频的长度和宽度
        int[] width = new int[1];
        int[] height = new int[1];
           /* mSps=Arrays.copyOfRange(spspps,0,pos);
            mPps= Arrays.copyOfRange(spspps,pos,spspps.length);*/
        AvcUtils.parseSPS(this.cur_sps, width, height);//从sps中解析出视频宽高
        format= MediaFormat.createVideoFormat("video/avc", width[0], height[0]);
        Log.i(TAG, "width:" + width[0] + "; height:" + height[0]);
        //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width[0], height[0] * mWidth);
        //format.setInteger(MediaFormat.KEY_MAX_HEIGHT, mHeight);
        //format.setInteger(MediaFormat.KEY_MAX_WIDTH, mWidth);
        format.setInteger(MediaFormat.KEY_BIT_RATE, width[0]*height[0]);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        format.setByteBuffer("csd-0", ByteBuffer.wrap(this.cur_sps));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(this.pps));
        try {
            decoder = MediaCodec.createDecoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        decoder.configure(format, mSurface, null, 0);
        //配置MediaMuxer


        decoder.start();
        return true;
    }

    public void start(Context context) {
        Log.d(TAG, "decoder线程启动");
        if (!config(context)) {
            Log.d(TAG, "视频解码器初始化失败");
            isRunning = false;
        }
        Log.i(TAG, "解码开始isRunning：" +isRunning );
        if (mWorker == null) {
            mWorker = new Worker();
           // mWorker.setRunning(true);
            mWorker.start();
        }
    }

    public void stop() {
        if (mWorker != null) {
           // mWorker.setRunning(false);
            mWorker = null;
        }
        //AVAPIsClient.close();
        decoder.flush();
        decoder.stop();
        decoder.release();
        this.mSurface.release();
        this.mSurface=null;
        decoder = null;
        Log.d("release","successful release");

    }

    private static class Worker extends Thread {
        MediaCodec.BufferInfo mBufferInfo= new MediaCodec.BufferInfo();
       /* public void setRunning(boolean running) {
            isRunning = running;
        }*/
        @Override
        public void run() {

            while (true) {
                try {
                    decode();
                }catch (IllegalStateException e){
                    Log.i(TAG,"被中断");
                    return;
                }

            }
            //release();

        }

        private void decode() {
            boolean isEOS = false;
            BufferInfo bufferInfo;
            while(true) {
                //判断是否是流的结尾
                bufferInfo = AVAPIsClient.readFrame();
                int inIndex = decoder.dequeueInputBuffer(-1);
                Log.i("decode", "解码开始：" + inIndex);
                if (inIndex >= 0) {
                    /**
                     * 测试
                     */
//                    byte[] frame=mServer.readFrame();
                    //获取数据流
                    //Frame frame = mServer.readFrameWidthCache();
                    Log.i("decode", "解码开始bufferInfo大小：" + bufferInfo.len);
                    ByteBuffer buffer = decoder.getInputBuffer(inIndex);
                    if (buffer == null) {
                        Log.d("decode", "getInputBuffer is null");
                        return;
                    }
                    buffer.clear();
                    if (bufferInfo == null) {
                        Log.d("decode", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        //isEOS = true;
                        //isRunning = false;
                        //服务已经断开，释放服务端
                        //mServer.release();
                    } else {
                        //录制
                        if (isNeedRecord) {
                            Log.i("Muxer", "录制");
                            ByteBuffer byteBuffer = ByteBuffer.wrap(bufferInfo.buffer, 0, bufferInfo.len);
                            videoTrackInfo.presentationTimeUs = System.nanoTime() / 1000;
                            videoTrackInfo.offset = 0;
                            videoTrackInfo.size = bufferInfo.len;
                            videoTrackInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                            if (bufferInfo.isIframe) {
                                Log.i("Muxer", "关键帧");
                                videoTrackInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
//                    System.out.println("I " + videoTrackInfo.size);
                            } else {
                                videoTrackInfo.flags = MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
//                    System.out.println("P " + videoTrackInfo.size);
                            }
                            mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, videoTrackInfo);
                        }
                        Log.d("decode", "填入数据");
                        //加水印
                        // 叠加时间水印
                        //YuvUtils.addYuvOsd(bufferInfo.buffer, 300, 150,true, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), 200, 200);
                        buffer.put(bufferInfo.buffer, 0, bufferInfo.len);
                        buffer.clear();
                        //buffer.limit(bufferInfo.len);
                        decoder.queueInputBuffer(inIndex, 0, bufferInfo.len, System.nanoTime() / 1000, 0);
                    }


                    int outIndex = decoder.dequeueOutputBuffer(mBufferInfo, 0);
                    Log.d("decode", "video decoding .....outIndex：" + outIndex);
                    while (outIndex >= 0) {
                        Log.d("decode", "解码");
                        ByteBuffer bufferss = decoder.getOutputBuffer(outIndex);
                        decoder.releaseOutputBuffer(outIndex, true);
                        outIndex = decoder.dequeueOutputBuffer(mBufferInfo, 0);//再次获取数据，如果没有数据输出则outIndex=-1 循环结束
                    }
                }
            }


        }

        /**
         * 释放资源
         */
        private void release() {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }
        }
    }
}
