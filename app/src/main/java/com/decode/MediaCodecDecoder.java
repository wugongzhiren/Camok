package com.decode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.camera.model.api.AVAPIsClient;
import com.decode.tools.AvcUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


/**
 * 解码器
 */

public class MediaCodecDecoder {
    public static final int TRY_AGAIN_LATER = -1;
    public static final int BUFFER_OK = 0;
    public static final int BUFFER_TOO_SMALL = 1;
    public static final int OUTPUT_UPDATE = 2;

    private int j=0;
    private final String MIME_TYPE = "video/avc";
    private MediaCodec mMC = null;
    private MediaFormat mMF;
    private long iBUFFER_TIMEOUT = -1;//0则立即返回，-1则无限等待直到有可使用的缓冲区，大于0，则等待时间为传入的毫秒值。
    private long oBUFFER_TIMEOUT = 0;
    private MediaCodec.BufferInfo mBI;//用于描述解码得到的byte[]数据的相关信息
    private MediaMuxer mMediaMuxer;
    private Surface surface;
    private int videoTrackIndex;
    private byte[] sps= {0,0,0,1,103,77,0,30,-107,-88,40,11,-2,89,-72,8,8,8,16};
    private byte[] sps_hd={0,0,0,1,103,77,0,31,-107,-88,20,1,110,-101,-128,-128,-128,-127};
    private byte[] cur_sps;
    private byte[] pps= {0,0,0,1,104,-18,60,-128/*,0,0,0,1,6,-27,1,91,-128*/};

    public MediaCodecDecoder(){
        byte quanlity = AVAPIsClient.nowQuality;
        System.out.println("now q is:"+ quanlity);
        if(quanlity == AVAPIsClient.AVIOCTRL_QUALITY_MIN
                || quanlity == AVAPIsClient.AVIOCTRL_QUALITY_LOW
                || quanlity == AVAPIsClient.AVIOCTRL_QUALITY_MIDDLE
                || quanlity==-1){
            cur_sps=sps;
        } else if(quanlity == AVAPIsClient.AVIOCTRL_QUALITY_HIGH ){
            cur_sps=sps_hd;
        }else{
            return;
        }
    }
    public int setQuanlity(int quanlity){
        mMC.stop();
        Log.d("set","stop()");
        if(quanlity==0 || quanlity==1 || quanlity==2 ){
            cur_sps=sps;
            Log.d("set","q 012");

        }else if(quanlity==3){
            cur_sps=sps_hd;
            Log.d("set","q 3");

        }else {
            return TRY_AGAIN_LATER;
        }
        configure(surface);
        Log.d("set","config");

        start();
        Log.d("set","start()");

        return 0;
    }

    /**
     * 初始化解码器
     * @throws IOException 创建解码器失败会抛出异常
     */
    public void init() throws IOException {
        mMC = MediaCodec.createDecoderByType(MIME_TYPE);
    }

    /**
     * 配置解码器
     * @param surface 用于解码显示的Surface
     */
    public void configure(Surface surface){
        this.surface=surface;
/*        try {
            fc=  new FileOutputStream(Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" +
                    "ScreenRecord" + "/1.mp4").getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/

        int[] width = new int[1];
        int[] height = new int[1];
        AvcUtils.parseSPS(this.cur_sps, width, height);//从sps中解析出视频宽高
        Log.i("videoInfo","width:"+width[0]+"; height:"+height[0]);

        mMF = MediaFormat.createVideoFormat(MIME_TYPE, width[0], height[0]);

        mMF.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMF.setByteBuffer("csd-0", ByteBuffer.wrap(this.cur_sps));
        mMF.setByteBuffer("csd-1", ByteBuffer.wrap(this.pps));
        mMF.setInteger(MediaFormat.KEY_BIT_RATE, width[0]*height[0]);

        mMC.configure(mMF, surface, null, 0);
      /* String mDstPath= Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/" +
                "ScreenRecord" + "/1.mp4";
        try {
            mMediaMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrackIndex = mMediaMuxer.addTrack(mMF);
            mMediaMuxer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }


    /**
     * 开启解码器，获取输入输出缓冲区
     */
    public void start(){
        mMC.start();
    }

    /**
     * 输入数据
     * @param data 输入的数据
     * @param len 数据有效长度
     * @param timestamp 时间戳
     * @return 成功则返回{@link #BUFFER_OK} 否则返回{@link #TRY_AGAIN_LATER}
     */
    public int input(byte[] data,int len,long timestamp){
        int i = mMC.dequeueInputBuffer(iBUFFER_TIMEOUT);
//            Log.i("input","index:"+i);
            //填充数据到输入流
            if(i >= 0){
                ByteBuffer inputBuffer = mMC.getInputBuffers()[i];
                inputBuffer.clear();
                // 输入数据
                inputBuffer.put(data, 0, len);
                /**
                 * queueInputBuffer第三个参数是时间戳，按时间线性增加
                 * 后面一段的代码就是把缓 冲区给释放掉，因为我们直接让解码器显示，就不需要解码出来的数据了，但是必须要这么释放一下，否则解码器始终给你留着，内存就该不够用了。
                 */
                mMC.queueInputBuffer(i, 0, len, timestamp, 0);
            }else {
               return TRY_AGAIN_LATER;
            }
            return BUFFER_OK;
    }


    // 解码数据到surface
   // private  boolean isFirstOpenConnected=true;
    public int output(){
        mBI = new MediaCodec.BufferInfo();
        int i = mMC.dequeueOutputBuffer(mBI, oBUFFER_TIMEOUT);// 把转码后的数据存到mBI
        while(i >= 0){
           ByteBuffer outputBuffer =mMC.getOutputBuffers()[i];
           /* if(isFirstOpenConnected){
              *//*  Message message=Message.obtain();
                message.arg1=3;
                handler.sendMessage(message);*//*
            }*/
           // isFirstOpenConnected=false;
           // ByteBuffer outputBuffer =mMC.getOutputBuffers()[i];
            /*writeSampleData(videoTrackIndex,mBI,outputBuffer);
            if(j>500){
                releaseMuxer();
            }*/
            /**
             * 获取输出数据
             * 第二个参数设置为true，表示解码显示在Surface上
             */
            mMC.releaseOutputBuffer(i, true);
            i = mMC.dequeueOutputBuffer(mBI, oBUFFER_TIMEOUT);
        }
        return BUFFER_OK;
    }

    private void writeSampleData(int track,MediaCodec.BufferInfo buffer, ByteBuffer encodedData) {
        Log.d("writeSampleData", "buffer.presentationTimeUs="+buffer.presentationTimeUs);
        Log.d("writeSampleData", "buffer.flags="+buffer.flags);
        Log.d("writeSampleData", "MediaCodec.BUFFER_FLAG_CODEC_CONFIG="+MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            Log.d("writeSampleData", "Ignoring BUFFER_FLAG_CODEC_CONFIG");
            buffer.size = 0;
        }
        boolean eos = (buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if (buffer.size == 0 && !eos) {
            Log.d("writeSampleData", "info.size == 0, drop it.");
            encodedData = null;
        } else {
           /* if (buffer.presentationTimeUs != 0) { // maybe 0 if eos

                    resetVideoPts(buffer);
            }*/

           Log.d("writeSampleData", "[" + Thread.currentThread().getId() + "] Got buffer, track=" + track
                        + ", info: size=" + buffer.size
                        + ", presentationTimeUs=" + buffer.presentationTimeUs);
        }
        if (encodedData != null) {
            encodedData.position(buffer.offset);
            encodedData.limit(buffer.offset + buffer.size);
            mMediaMuxer.writeSampleData(track, encodedData, buffer);

            Log.i("writeSampleData", "Sent " + buffer.size + " bytes to MediaMuxer on track " + track);
        }
    }

    public void flush(){
        mMC.flush();
    }

    public void release() {
        flush();
        mMC.stop();
        mMC.release();
        this.surface.release();
        this.surface=null;
        mMC = null;
        Log.d("release","successful release");
    }
    public void releaseMuxer() {
        flush();
        mMC.stop();
        mMC.release();
//        mMC = null;
        Log.d("release","successful release");
    }
}


