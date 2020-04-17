package com.zhuangliming.camok;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.camera.camerawithtutk.VideoThread;
import com.camera.model.VideoInfo;
import com.camera.model.api.AVAPIsClient;
import com.decode.EncoderVideoRunnable;
import com.decode.MediaCodecDecoder;
import com.decode.VideoDecoder;
import com.decode.tools.AvcUtils;
import com.decode.tools.BufferInfo;
import com.hankvision.ipcsdk.Dllipcsdk;
import com.hankvision.ipcsdk.JMultipleOsdInfo;
import com.hankvision.ipcsdk.JOSDInfo;
import com.hankvision.ipcsdk.JTimeOsdInfo;
import com.zhuangliming.FFmpegKit;
import com.zhuangliming.camok.model.MessageEvent;
import com.zhuangliming.camok.video.FFmpegCommandCenter;
import com.zhuangliming.camok.view.MediaPopView;
import com.zhuangliming.camok.view.OsdPopView;
import com.zhuangliming.camok.view.PreViewPhotoPopView;
import com.zhuangliming.camok.view.PreViewVideoPopView;
import com.zhuangliming.camok.view.UUIDPopView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;

public class MainActivity extends Activity implements Dllipcsdk.CBRawData, View.OnClickListener,TextureView.SurfaceTextureListener {
    static {
        System.loadLibrary("native-lib");
    }

    public static final int REQUEST_MEDIA_PROJECTION = 18;
    int oldCam = 0;
    String strIp = "192.168.1.18";
    String strUsername = "admin";
    String strPassword = "admin";
    int nHttpPort = 80;
    int nVideoPort = 90;

    static final String UID = "41R82E1J52USSEHZ111A";
    //static final String UID = "EX2FBRGRS4PRWKVD111A";
    public static final int TRY_AGAIN_LATER = -1;
    public static final int BUFFER_OK = 0;
    public static final int BUFFER_TOO_SMALL = 1;
    public static final int OUTPUT_UPDATE = 2;
    ArrayList<VideoInfo> videoInfoArrayList = new ArrayList<>();

    private final String MIME_TYPE = "video/avc";
    private MediaCodec mMC = null;
    private MediaFormat mMF;
    private long iBUFFER_TIMEOUT = -1;//0则立即返回，-1则无限等待直到有可使用的缓冲区，大于0，则等待时间为传入的毫秒值。
    private long oBUFFER_TIMEOUT = 0;
    private MediaCodec.BufferInfo mBI;//用于描述解码得到的byte[]数据的相关信息

    private Surface surface;
    private byte[] sps = {0, 0, 0, 1, 103, 77, 0, 30, -107, -88, 40, 11, -2, 89, -72, 8, 8, 8, 16};
    private byte[] sps_hd = {0, 0, 0, 1, 103, 77, 0, 31, -107, -88, 20, 1, 110, -101, -128, -128, -128, -127};
    private byte[] cur_sps;
    private byte[] pps = {0, 0, 0, 1, 104, -18, 60, -128/*,0,0,0,1,6,-27,1,91,-128*/};

    private Thread t1;
    private boolean isConnected=false;
    MediaCodecDecoder mediaCodecDecoder; //解码器
    public static BlockingDeque<BufferInfo> bq;
    public BlockingDeque<BufferInfo> xbq;
    public IoCtrl ioCtrl;
    private Button buttonConnect;
    private ImageView imageViewLed2;
    private ImageView imageViewOSD;
    private ImageView imageViewSettings;
    private ImageButton recordBt;
    private ImageView editOsdImg;
    private ImageButton ButtonZoomTele;
    private ImageButton ButtonZoomWide;
    private ImageButton ButtonMotorUp;
    private ImageButton ButtonMotorDown;
    private ImageButton screenCapBt;
    private ImageView columImg;
    //private RadioButton radioButtonCamType;
    private TextureView textureView;
    private LinearLayout leftTool;
    private LinearLayout rightTool;
    private LinearLayout parentView;
    private LinearLayout osdParent;
    private TextView taskNameTx;
    private TextView wellNameTx;
    private TextView checkInfoTx;
    private TextView checkCompanyTx;
    private LinearLayout recordInfo;

    private void showPopupMenu(View view) {
        // View当前PopupMenu显示的相对View的位置
        PopupMenu popupMenu = new PopupMenu(this, view);
        // menu布局
        popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());
        // menu的item点击事件
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId()==R.id.settings){
                    displayUIDPop();
                }
                if(item.getItemId()==R.id.regard){
                    System.exit(0);
                }
                return false;
            }
        });
        // PopupMenu关闭事件
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {

                //Toast.makeText(getApplicationContext(), "关闭PopupMenu", Toast.LENGTH_SHORT).show();
            }
        });

        popupMenu.show();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //注册EventBus
        EventBus.getDefault().register(this);
        initView();
        initEvent();
        showOSD();
        ButtonZoomTele.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean bRet;
                int lcurrentTimeMillis = Integer.parseInt(String.valueOf(1585958888));
                bRet = Dllipcsdk.IPCNET_SetCameraTime(strIp, 90, lcurrentTimeMillis, strUsername, strPassword);
                Log.i("设置时间", bRet + "");
                if (bRet == true) {
                    System.out.println("IPCNET_SetCameraTime:  ----- " + lcurrentTimeMillis);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    AVAPIsClient.setOSD();
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_LENS_ZOOM_IN, (byte) 1);
                    //bRet = Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_TELE.ordinal(), 6, 6, false);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_PTZ_STOP, (byte) 0);
                    //Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_TELE.ordinal(), 6, 6, true);

                }
                return false;
            }
        });

        ButtonZoomWide.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean bRet;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_LENS_ZOOM_OUT, (byte) 1);
                    //bRet = Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_WIDE.ordinal(), 6, 6, false);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_PTZ_STOP, (byte) 0);
                    //Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_WIDE.ordinal(), 6, 6, true);

                }
                return false;
            }
        });

        byte[] buffer = new byte[4];
        buffer[0] = 0x0a;
        buffer[3] = (byte) 0xe0;
        buffer[1] = 0x02;
        ButtonMotorUp.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    buffer[2] = 1;

                    TaskCenter.sharedCenter().send(buffer);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    buffer[2] = 0;
                    TaskCenter.sharedCenter().send(buffer);
                }
                return false;
            }
        });


        ButtonMotorDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    buffer[2] = 2;

                    TaskCenter.sharedCenter().send(buffer);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    buffer[2] = 0;
                    TaskCenter.sharedCenter().send(buffer);
                }
                return false;
            }
        });
        /*SetParam(osdInfoTx);*/
    }

    private void initView() {
        imageViewSettings = (ImageView) findViewById(R.id.imageSettings);
        //registerForContextMenu(imageViewSettings);
        buttonConnect = findViewById(R.id.button);
        imageViewLed2 = (ImageView) findViewById(R.id.buttonLED2);
        ButtonZoomTele = (ImageButton) findViewById(R.id.imageViewZOOM_TELE);
        ButtonZoomWide = (ImageButton) findViewById(R.id.imageViewZOOM_WIDE);
        ButtonMotorUp = (ImageButton) findViewById(R.id.imageViewMotorUp);
        recordBt=findViewById(R.id.record);
        editOsdImg=findViewById(R.id.editOsd);
        ButtonMotorDown = (ImageButton) findViewById(R.id.imageViewMotorDown);
       // radioButtonCamType = (RadioButton) findViewById(R.id.radioButton);
        columImg=findViewById(R.id.imageView2);
        screenCapBt = findViewById(R.id.imageView9);
        osdParent=findViewById(R.id.textInfoParent);
        parentView=findViewById(R.id.parent);
        textureView = findViewById(R.id.frame);
        taskNameTx=findViewById(R.id.taskNameTx);
        wellNameTx=findViewById(R.id.wellNameTx);
        checkInfoTx=findViewById(R.id.checkInfoTx);
        checkCompanyTx=findViewById(R.id.checkCompanyTx);
        imageViewOSD=findViewById(R.id.imageViewOSD);
        leftTool=findViewById(R.id.leftTool);
        rightTool=findViewById(R.id.rightTool);
        recordInfo=findViewById(R.id.recordInfo);
        //textureView.setSurfaceTextureListener(this);
        /*glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new MyRender());
        *//*渲染方式，RENDERMODE_WHEN_DIRTY表示被动渲染，只有在调用requestRender或者onResume等方法时才会进行渲染。RENDERMODE_CONTINUOUSLY表示持续渲染*//*
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);*/
        //实例化解码器
        //mediaCodecDecoder = new MediaCodecDecoder();
        // 初始化
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("connectIPC","connectIPC auto");
                //StartRaw(buttonConnect);

            }
        },2000);
        // 此线程从阻塞队列poll buffer信息并送入解码
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.editOsd:
                displayDialog();
                break;
            case R.id.imageViewOSD:
                int osdShow=OsdSharePreference.getInstance(this).getInt("osd",0);
                if(0==osdShow){
                    setOsdInfo();
                    osdParent.setVisibility(View.VISIBLE);
                    OsdSharePreference.getInstance(this).putInt("osd",1);
                }else{
                    osdParent.setVisibility(View.GONE);
                    OsdSharePreference.getInstance(this).putInt("osd",0);
                }
                break;
            case R.id.imageView2:
                //打开媒体
                displayMediaPop();
                break;
            case R.id.imageSettings:
                showPopupMenu(v);
        }
    }

    private void initEvent() {
        //截图
        screenCapBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* int nPicBuffer = 2 * 1024 * 1024;
                byte[] pPicBuffer = new byte[nPicBuffer];
                int nPicLenth = Dllipcsdk.IPCNET_CapturePicture(strIp, nHttpPort, strUsername, strPassword, pPicBuffer, nPicBuffer);
                Log.i("截图", "nPicLenth:" + nPicLenth);
                if (nPicLenth > 0) {
                    System.out.println("IPCNET_CapturePicture:  ----- " + nPicLenth);
                }*/
                //StartRecord(v);
                takeScreenShot();
            }
        });
        recordBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(AVAPIsClient.mMuxerUtils.isMuxerStarted()){
                    recordBt.setBackground(getResources().getDrawable(R.drawable.shexiang));
                    Toast.makeText(MainActivity.this,"结束录制",Toast.LENGTH_SHORT).show();
                    recordInfo.setVisibility(View.GONE);
                    //AVAPIsClient.mMuxerUtils.se()=false;
                    AVAPIsClient.mMuxerUtils.stopMuxer();
                }else{
                    recordBt.setBackground(getResources().getDrawable(R.drawable.shexiang_2));
                    Toast.makeText(MainActivity.this,"开始录制",Toast.LENGTH_SHORT).show();
                    recordInfo.setVisibility(View.VISIBLE);
                    //AVAPIsClient.mMuxerUtils.isRecord=true;
                    AVAPIsClient.mMuxerUtils.startMuxer();
                }

            }
        });
        editOsdImg.setOnClickListener(this);
        imageViewOSD.setOnClickListener(this);
        columImg.setOnClickListener(this);
        textureView.setSurfaceTextureListener(this);
        imageViewSettings.setOnClickListener(this);
    }

    public void showOSD(){
        if(isConnected&&OsdSharePreference.getInstance(this).getInt("osd",0)==1){
            setOsdInfo();
            osdParent.setVisibility(View.VISIBLE);
        }
    }

    private void addOsdInfoToVideo(String videoUrl){

        String textMark="测试文字";
         final String dirpath =
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/TUTK_VIDEOS";
        SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);
        final String path = dirpath + "/" + df.format(new Date()) + "111.mp4";
        String[] commands= FFmpegCommandCenter.addTextMark(textMark,videoUrl,path);
        final String[] _commands=commands;
        FFmpegKit.execute(_commands);
        /*Runnable compoundRun=new Runnable() {
            @Override
            public void run() {

        };*/
       // ThreadPoolUtils.execute(compoundRun);
    }
    //辅助灯
    private boolean led2 = false;

    public void onClickOpenLed2(View view) {
        byte[] buffer = new byte[4];
        buffer[0] = 0x0a;
        buffer[3] = (byte) 0xe0;
        buffer[1] = 0x04;
        if (led2 == true) {
            buffer[2] = 0;
            led2 = false;
            //imageViewLed2.setImageResource(R.drawable.deng4_1);
            VideoThread.startReceive = false;
        } else {
            buffer[2] = 1;
            //imageViewLed2.setImageResource(R.drawable.deng4);
            led2 = true;
            VideoThread.startReceive = true;
        }
        TaskCenter.sharedCenter().send(buffer);
        Toast.makeText(MainActivity.this, "指令下达", Toast.LENGTH_SHORT).show();
    }


    /**
     * 链接摄像头
     */
    int camConnect = 0;

    /*public void connectIPC() {
        //--------------------------------------- 解码 ------------------------------------
        // 此线程从阻塞队列poll buffer信息并送入解码器
        t1 = new Thread(() -> {
            BufferInfo temp;
            while (true) {
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Java技术栈线程被中断，程序退出。");
                    return;
                }
                try {
                    temp = bq.poll(3000, TimeUnit.MILLISECONDS);
                    if (temp == null) {
                        continue;
                    }
                    // 向解码器输入buffer
                    SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);
                    Log.i("时间入",df.format(new Date()));
                    mediaCodecDecoder.input(temp.buffer, temp.len, System.nanoTime() / 1000);

                    mediaCodecDecoder.output();
                    Log.i("时间出",df.format(new Date()));
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }
        });
        t1.start();


        // surfaceView绘制完成后 配置解码器并启动
      *//*  glSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                // 配置解码器
                mediaCodecDecoder.configure(glSurfaceView.getHolder().getSurface());
                System.out.println("配置解码器");
                // 启动解码器
                mediaCodecDecoder.start();
                System.out.println("启动解码器");
            }
            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mediaCodecDecoder.release();
            }
        });*//*

        // mediaCodecDecoder.configure(surfaceViewDecode.getHolder().getSurface());
        //System.out.println("配置解码器");
        // 启动解码器
        // mediaCodecDecoder.start();


       *//* SetParam(osdInfoTx);*//*
    }
*/

    public void onClickOpen(View view) throws IOException {

        if (camConnect == 1) {
            Toast.makeText(MainActivity.this, "摄像机已经链接", Toast.LENGTH_SHORT).show();
            return;
        }
        //connectIPC();
        //buttonConnect.setBackgroundColor(0xff0320);
        camConnect = 1;
    }

    public void MediaCodecDecoder() {
        byte quanlity = AVAPIsClient.nowQuality;
        System.out.println("now q is:" + quanlity);
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


    public int setQuanlity(int quanlity) {
        mMC.stop();
        Log.d("set", "stop()");
        if (quanlity == 0 || quanlity == 1 || quanlity == 2) {
            cur_sps = sps;
            Log.d("set", "q 012");

        } else if (quanlity == 3) {
            cur_sps = sps_hd;
            Log.d("set", "q 3");

        } else {
            return TRY_AGAIN_LATER;
        }
        //弃用
       /* configure(surface);
        Log.d("set", "config");

        //start();
        Log.d("set", "start()");*/

        return 0;
    }


    /**
     * 初始化解码器
     *
     * @throws IOException 创建解码器失败会抛出异常
     */
    public void init() throws IOException {
        mMC = MediaCodec.createDecoderByType(MIME_TYPE);
    }

    /**
     * 配置解码器
     *
     * @param surface 用于解码显示的Surface
     */
    public void configure(Surface surface) {
        this.surface = surface;
        int[] width = new int[1];
        int[] height = new int[1];
        AvcUtils.parseSPS(this.cur_sps, width, height);//从sps中解析出视频宽高
        Log.i("videoInfo", "width:" + width[0] + "; height:" + height[0]);

        mMF = MediaFormat.createVideoFormat(MIME_TYPE, width[0], height[0]);

        mMF.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mMF.setByteBuffer("csd-0", ByteBuffer.wrap(this.cur_sps));
        mMF.setByteBuffer("csd-1", ByteBuffer.wrap(this.pps));
        mMF.setInteger(MediaFormat.KEY_BIT_RATE, width[0] * height[0]);

        mMC.configure(mMF, surface, null, 0);
    }


    /**
     * 开启解码器，获取输入输出缓冲区
     */
    public void start() {
        mMC.start();
    }

    /**
     * 输入数据
     *
     * @param data      输入的数据
     * @param len       数据有效长度
     * @param timestamp 时间戳
     * @return 成功则返回{@link #BUFFER_OK} 否则返回{@link #TRY_AGAIN_LATER}
     */
    public int input(byte[] data, int len, long timestamp) {
        int i = mMC.dequeueInputBuffer(iBUFFER_TIMEOUT);
//            Log.i("input","index:"+i);
        //填充数据到输入流
        if (i >= 0) {
            ByteBuffer inputBuffer = mMC.getInputBuffers()[i];
            inputBuffer.clear();
            // 输入数据
            inputBuffer.put(data, 0, len);
            /**
             * queueInputBuffer第三个参数是时间戳，按时间线性增加
             * 后面一段的代码就是把缓 冲区给释放掉，因为我们直接让解码器显示，就不需要解码出来的数据了，但是必须要这么释放一下，否则解码器始终给你留着，内存就该不够用了。
             */
            mMC.queueInputBuffer(i, 0, len, timestamp, 0);
        } else {
            return TRY_AGAIN_LATER;
        }
        return BUFFER_OK;
    }


    // 解码数据到surface
    public int output() {
        mBI = new MediaCodec.BufferInfo();
        int i = mMC.dequeueOutputBuffer(mBI, oBUFFER_TIMEOUT);// 把转码后的数据存到mBI
//        Log.i("output","index:"+i);

        while (i >= 0) {
            ByteBuffer outputBuffer = mMC.getOutputBuffers()[i];
            /**
             * 获取输出数据
             * 第二个参数设置为true，表示解码显示在Surface上
             */
            mMC.releaseOutputBuffer(i, true);
            i = mMC.dequeueOutputBuffer(mBI, oBUFFER_TIMEOUT);
        }
        return BUFFER_OK;
    }

    public void flush() {
        mMC.flush();
    }

    public void release() {
        flush();
        mMC.stop();
        mMC.release();
//        mMC = null;
        Log.d("release", "successful release");
    }

    /**
     * 保存参数
     */
    private void saveUserInfo() {
        SharedPreferences userInfo = getSharedPreferences(UID, MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfo.edit();
        System.out.print("saveUserInfo\n");
        editor.putString("name", "admin");
        editor.putString("key", "123456");
        editor.putString("uid", "41R82E1J52USSEHZ111A");
        editor.apply();
    }

    private void getUserInfo() {
        SharedPreferences userInfo = getSharedPreferences(UID, MODE_PRIVATE);
        System.out.print("getUserInfo\n");
        String name;
        name = userInfo.getString("name", null);
        System.out.println(name);
    }

    private void setOsdInfo(){
        taskNameTx.setText(OsdSharePreference.getInstance(MainActivity.this).getString("taskname"));
        wellNameTx.setText(OsdSharePreference.getInstance(MainActivity.this).getString("wellname"));
        checkInfoTx.setText(OsdSharePreference.getInstance(MainActivity.this).getString("checkinfo"));
        checkCompanyTx.setText(OsdSharePreference.getInstance(MainActivity.this).getString("checkcompany"));
    }

    /*
     * 以下代码演示取视频流
     * StartRaw开启视频、StopRaw关闭视频、RawData回调
     * 返回值如果等于-1，则表示出错
     * 记得开启网络权限
     * */
    public void RawData(int lRawHandle, int nErrorType, int nErrorCode, int m_AVType, int m_EncoderType, int m_FrameType, int m_FrameRate,
                        int m_VideoWidth, int m_VideoHeight, int m_Channels, int m_Samples, int m_BitCount, int m_TimeStamp,
                        byte pRawBuffer[], int lRawBufSize) {
        // TODO 自动生成的方法存根

        if (nErrorType == 0) {
            String str = String.format("%x %d %d %d %d %d %d %d %d %d %d %d %d\n", lRawHandle, lRawBufSize, m_AVType, m_EncoderType, m_FrameType,
                    m_FrameRate, m_VideoWidth, m_VideoHeight, m_Channels, m_Samples, m_BitCount, m_TimeStamp, lRawBufSize);

            //System.out.println(str);

            // 在此处接收码流
        } else if (nErrorType < 0) {
            String str;
            if (nErrorType == -1000) {
                str = String.format("接设备失败:%d_%d\n", nErrorType, nErrorCode);
            } else if (nErrorType == -999) {
                str = String.format("登录设备失败，用户名或密码错误:%d_%d\n", nErrorType, nErrorCode);
            } else if (nErrorType == -998) {
                str = String.format("请求视频失败，一般为设备原因:%d_%d\n", nErrorType, nErrorCode);
            } else {
                str = String.format("其它错误:%d_%d\n", nErrorType, nErrorCode);
            }
        }
    }

    long lRawHandle = -1;

    public void StartRaw(View v) {
        if(isConnected){
            return;
        }
        buttonConnect.setText("正在连接");
        /*if (radioButtonCamType.isChecked()) {
            oldCam = 0;
        }*/
        ioCtrl.initTCP();
        if (oldCam == 0) {
            //getUserInfo();
            //saveUserInfo();
/*
            bq = new LinkedBlockingDeque<>();// videobuffer信息存储到这里 解码器从此阻塞队列poll video的信息
            (new Thread() {
                public void run() {
                    int stu = AVAPIsClient.start(MainActivity.this.UID, bq);
                    System.out.println("x连接线程中断++++++");
                }
            }).start();*/

            //connectIPC();
            return;
        }
        System.out.println("StartRaw");
        if (lRawHandle == -1) {

            lRawHandle = Dllipcsdk.IPCNET_StartRawData(strIp, nVideoPort, 0, strUsername, strPassword, 1, this);
        }
        if (lRawHandle != -1) {
            buttonConnect.setText("已链接");
            buttonConnect.setBackgroundColor(0xff00ff00);

            StartPlay(v);
        }
    }

    public void StopRaw(View v) {

        if (lRawHandle != -1) {

            Dllipcsdk.IPCNET_StopRawData(lRawHandle);

            lRawHandle = -1;
        }
    }

    /*
     * 以下代码演示实时预览
     * StartPlay开始预览、StopPlay停止预览
     * 返回值如果等于-1，则表示出错
     * 记得开启网络权限
     * */
    long lPlay = -1;

    public void StartPlay(View v) {

        if (lPlay == -1) {
          //SurfaceView surfaceView = findViewById(R.id.surfaceView);
            /*if (mSurface != null) {
                lPlay = Dllipcsdk.IPCNET_StartRawPlay(strIp, nVideoPort, 0, "admin", "admin", 1, mSurface);
            }*/
        }
    }

    public void StopPlay(View v) {

        if (lPlay != -1) {

            Dllipcsdk.IPCNET_StopRawPlay(lPlay);

            lPlay = -1;
        }
    }

    public void SetParam(View v) {

        boolean bRet;
        /*
         * 演示获取和设置OSD显示状态、位置、大小、文字
         * nIndex:第几行的数据，取值范围：0~4
         * 记得开启网络权限
         * */
        Log.i("SetParam", "开始获取OSD");
        JOSDInfo josdInfo = Dllipcsdk.IPCNET_GetOsdInfo(strIp, nHttpPort, 0, strUsername, strPassword);
        if (josdInfo != null) {
            Log.i("SetParam", "josdInfo:  ----- " + josdInfo.TextOSDTitle);
            //System.out.println("josdInfo:  ----- " + josdInfo.TextOSDTitle);

            Dllipcsdk.IPCNET_SetOsdInfo(strIp, nHttpPort, 0, josdInfo, strUsername, strPassword);
        }

        /*
         * 演示获取和设置倍数显示状态、位置、大小
         * 记得开启网络权限
         * */
        JMultipleOsdInfo jMultipleOsdInfo = Dllipcsdk.IPCNET_GetMultipleOsdInfo(strIp, nHttpPort, strUsername, strUsername);
        if (jMultipleOsdInfo != null) {
            System.out.println("jMultipleOsdInfo:  ----- " + jMultipleOsdInfo.MultipleOSDStatus);

            Dllipcsdk.IPCNET_SetMultipleOsdInfo(strIp, nHttpPort, jMultipleOsdInfo, strUsername, strPassword);
        }

        /*
         * 演示获取和设置时间OSD显示状态、位置、大小
         * 记得开启网络权限
         * */
        JTimeOsdInfo jTimeOsdInfo = Dllipcsdk.IPCNET_GetTimeOsdInfo(strIp, nHttpPort, strUsername, strPassword);
        if (jTimeOsdInfo != null) {
            System.out.println("jTimeOsdInfo:  ----- " + jTimeOsdInfo.TimeOSDStatus);

            Dllipcsdk.IPCNET_SetTimeOsdInfo(strIp, nHttpPort, jTimeOsdInfo, strUsername, strPassword);
        }

        /*
         * 演示设置摄像机时间
         * 记得开启网络权限
         * */
        int lcurrentTimeMillis = Integer.parseInt(String.valueOf(System.currentTimeMillis() / 1000));
        bRet = Dllipcsdk.IPCNET_SetCameraTime(strIp, nHttpPort, lcurrentTimeMillis, strUsername, strPassword);
        if (bRet == true) {
            System.out.println("IPCNET_SetCameraTime:  ----- " + lcurrentTimeMillis);
        }

        /*
         * 演示获取和设置黑白
         *   nICRSwitch取值：
         *   0, "外部控制"
         *   1, "固定黑白"
         *   2, "固定彩色"
         *   3, "程序控制"
         * 记得开启网络权限
         * */
        int nICRSwitchInfo = Dllipcsdk.IPCNET_GetICRSwitchInfo(strIp, nHttpPort, strUsername, strPassword);
        if (nICRSwitchInfo != -1) {
            System.out.println("IPCNET_GetICRSwitchInfo:  ----- " + nICRSwitchInfo);

            Dllipcsdk.IPCNET_SetICRSwitchInfo(strIp, nHttpPort, nICRSwitchInfo, strUsername, strPassword);
        }

        /*
         * 演示抓拍
         * 返回值如果大于0，则为抓拍到的图片大小
         * 记得开启网络权限
         * */
        int nPicBuffer = 2 * 1024 * 1024;
        byte[] pPicBuffer = new byte[nPicBuffer];
        int nPicLenth = Dllipcsdk.IPCNET_CapturePicture(strIp, nHttpPort, strUsername, strPassword, pPicBuffer, nPicBuffer);
        if (nPicLenth > 0) {
            System.out.println("IPCNET_CapturePicture:  ----- " + nPicLenth);
        }

        /*
         * 演示PTZ控制，包括八个方向、聚焦、缩放、停止等
         * 每次调用完方向后，记得调用停止指令，否则的话，会一直转
         * 记得开启网络权限
         * */
        bRet = Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.UP.ordinal(), 6, 6, false);
        if (bRet == true) {
            System.out.println("IPCNET_PTZControl:  ----- " + Dllipcsdk.E_PTZ_COMMAND.UP.ordinal());
            Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.UP.ordinal(), 6, 6, true);
        }
    }

    /*
     * 以下代码演示录制MP4录像
     * StartRecord开始录像、StopRecord停止录像
     * 返回值如果等于-1，则表示出错
     * 记得开启网络权限和SD卡读写权限(SD卡读写权限需自己手动在权限管理中打开)
     * */
    long lRecord = -1;

    public void StartRecord(View v) {

        if (lRecord == -1) {
            int lcurrentTimeMillis = Integer.parseInt(String.valueOf(System.currentTimeMillis() / 1000));

            String mp4FilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM" + File.separator + "Video_" + lcurrentTimeMillis + ".mp4";

            lRecord = Dllipcsdk.IPCNET_StartRecord(strIp, nVideoPort, mp4FilePath, 0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i("录制", "停止录制");
                    StopRecord(null);
                }
            }, 5000);
            Log.i("StartRecord", "录制结果:" + lRecord);
        }
    }

    public void StopRecord(View v) {

        if (lRecord != -1) {

            Dllipcsdk.IPCNET_StopRecord(lRecord);

            lRecord = -1;
        }
    }


    /**
     * 截图
     * @return
     */
    public boolean takeScreenShot() {
        String imagePath = Environment.getExternalStorageDirectory() + File.separator +"TUTK_PHOTO"+ File.separator +"screenshot_"+System.currentTimeMillis()+".png";
        File file=new File(Environment.getExternalStorageDirectory() + File.separator +"TUTK_PHOTO");
        if(!file.exists()){
            file.mkdirs();
        }
        Bitmap mScreenBitmap =drawText2Bitmap(textureView.getBitmap(),this);
        try {
            FileOutputStream out = new FileOutputStream(imagePath);
            mScreenBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Toast.makeText(MainActivity.this,"截图成功",Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Log.i("screencap", "compress error");
            return false;
        }
    }

    /**
     * 截图处理，因为textureview无法捕获本身之外的view信息
     * @param bitmap
     * @param mContext
     * @return
     */
    public static Bitmap drawText2Bitmap(Bitmap bitmap, Context mContext) {

        //判断是否显示osd
        if(OsdSharePreference.getInstance(mContext).getInt("osd",0)==0){
            return bitmap;
        }
        try {

            Resources resources = mContext.getResources();
            float scale = resources.getDisplayMetrics().density;
            Bitmap.Config bitmapConfig = bitmap.getConfig();
            // set default bitmap config if none
            if (bitmapConfig == null) bitmapConfig = Bitmap.Config.ARGB_8888;
            // resource bitmaps are imutable, so we need to convert it to mutable one
            bitmap = bitmap.copy(bitmapConfig, true);

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // new antialised Paint
            paint.setColor(Color.rgb(255, 255, 255));       // text color - #3D3D3D
            paint.setTextSize((int)(12 * scale));           // text size in pixels
            paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY); // text shadow

            // draw text to the Canvas center
            Rect bounds = new Rect();
            //paint.getTextBounds(text, 0, text.length(), bounds);
            //int x = (bitmap.getWidth() - bounds.width()) / 6;
            //int y = (bitmap.getHeight() + bounds.height()) / 5;
            canvas.drawText("检测任务："+OsdSharePreference.getInstance(mContext).getString("taskname"), 30, 60, paint);
            canvas.drawText("井号信息："+OsdSharePreference.getInstance(mContext).getString("wellname"), 30, 110 , paint);
            canvas.drawText("检测信息："+OsdSharePreference.getInstance(mContext).getString("checkinfo"), 30 , 160, paint);
            canvas.drawText("检测单位："+OsdSharePreference.getInstance(mContext).getString("checkcompany"), 30 , 210 , paint);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    //private Surface mSurface;
    public VideoDecoder decoder;
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i("Decode","onSurfaceTextureAvailable");
        this.surface=new Surface(surface);
        AVAPIsClient.start(MainActivity.this.getApplicationContext(),this.surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i("TextureView","onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i("TextureView","onSurfaceTextureDestroyed");
       // decoder.stop();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.i("TextureView","onSurfaceTextureUpdated");
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity","onDestroy");
        //注销事件
        EventBus.getDefault().unregister(this);
        //mediaCodecDecoder.release();
       // t1.interrupt();

    }
    public void displayDialog(){
        OsdPopView myPopupWindow = new OsdPopView(this);
        myPopupWindow.showAtLocation(parentView, Gravity.CENTER,0,0);
        lightOff();

        /**
         * 消失时屏幕变亮
         */
        myPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

                layoutParams.alpha=1.0f;

                getWindow().setAttributes(layoutParams);
            }
        });
    }

    /**
     * 打开媒体文件库
     */
    public void displayMediaPop(){

        MediaPopView myPopupWindow = new MediaPopView(this);
        myPopupWindow.showAtLocation(parentView, Gravity.CENTER,0,0);
        lightOff();
        /**
         * 消失时屏幕变亮
         */
        myPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

                layoutParams.alpha=1.0f;

                getWindow().setAttributes(layoutParams);
            }
        });
    }

    /**
     * 打开媒体文件库
     */
    public void displayPhotoPop(String url){

        PreViewPhotoPopView myPopupWindow = new PreViewPhotoPopView(this,url);
        myPopupWindow.showAtLocation(parentView, Gravity.CENTER,0,0);
        /**
         * 消失时屏幕变亮
         */
        myPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

                layoutParams.alpha=1.0f;

                getWindow().setAttributes(layoutParams);
            }
        });
    }

    /**
     * 打开媒体文件库
     */
    public void displayVideoPop(String url){

        PreViewVideoPopView myPopupWindow = new PreViewVideoPopView(this,url);
        myPopupWindow.showAtLocation(parentView, Gravity.CENTER,0,0);
        /**
         * 消失时屏幕变亮
         */
        myPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

                layoutParams.alpha=1.0f;

                getWindow().setAttributes(layoutParams);
            }
        });
    }

    /**
     * 打开UID设置
     */
    public void displayUIDPop(){

        UUIDPopView myPopupWindow = new UUIDPopView(this);
        myPopupWindow.showAtLocation(parentView, Gravity.CENTER,0,0);
        lightOff();

        /**
         * 消失时屏幕变亮
         */
        myPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

                layoutParams.alpha=1.0f;

                getWindow().setAttributes(layoutParams);
            }
        });
    }

    /**
     * 显示时屏幕变暗
     */
    private void lightOff() {

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

        layoutParams.alpha=0.3f;

        getWindow().setAttributes(layoutParams);

    }

    /**
     * 处理eventbus消息
     * @param messageEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHandleEventMessage(MessageEvent messageEvent){
        if(messageEvent.getMessage()==MessageEvent.SHOW_PHOTO){
            //显示图片
            Log.i("onHandleEventMessage","收到了显示图片");
            displayPhotoPop((String) messageEvent.getObj());
        }else if(messageEvent.getMessage()==MessageEvent.PREVIEW_VIDEO){
            //预览视频
            AVAPIsClient.stopDecode();
            AVAPIsClient.close();
            //displayVideoPop(((MediaItem)msg.obj).url);
            Intent intent=new Intent(MainActivity.this,PLVideoViewActivity.class);
            intent.putExtra("videoPath",(String) messageEvent.getObj());
            startActivity(intent);
        }else if(messageEvent.getMessage()==MessageEvent.CONNECT_SUCCESS){
            //表示连接成功
            isConnected=true;
            showOSD();
            camConnect = 1;
            leftTool.setVisibility(View.VISIBLE);
            rightTool.setVisibility(View.VISIBLE);
            buttonConnect.setText("已连接");
            buttonConnect.setBackgroundColor(0xff00ff00);
        }else if(messageEvent.getMessage()==MessageEvent.RECORD_COMPLETE){
            //录制完成
            if(OsdSharePreference.getInstance(MainActivity.this).getInt("osd",0)==1){
                //显示字幕，水印处理
                addOsdInfoToVideo((String) messageEvent.getObj());
            }
        }else if(messageEvent.getMessage()==MessageEvent.SHOW_OSD){
            setOsdInfo();
            osdParent.setVisibility(View.VISIBLE);
        }else if(messageEvent.getMessage()==MessageEvent.DEVICE_CHANGE){
            //设备切换
            if(decoder.isNeedRecord){
                decoder.stopRecord();
            }
            AVAPIsClient.close();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //AVAPIsClient.start(MainActivity.this);
                }
            },2000);
        }else if(messageEvent.getMessage()==MessageEvent.NET_CONNECT){
            Toast.makeText(MainActivity.this,"网络已连接",Toast.LENGTH_SHORT).show();
            AVAPIsClient.start(getApplicationContext(),surface);
        }
        else if(messageEvent.getMessage()==MessageEvent.NET_LOSS){
            Toast.makeText(MainActivity.this,"网络断开",Toast.LENGTH_SHORT).show();
            //AVAPIsClient.stopDecode();
            AVAPIsClient.close();
        }
        else {
            return;
        }
    }
}
