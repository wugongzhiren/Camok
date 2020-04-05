package com.zhuangliming.cam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.camera.camerawithtutk.VideoThread;
import com.camera.model.VideoInfo;
import com.camera.model.api.AVAPIsClient;
import com.decode.MediaCodecDecoder;
import com.decode.tools.AvcUtils;
import com.decode.tools.BufferInfo;
import com.hankvision.ipcsdk.Dllipcsdk;
import com.hankvision.ipcsdk.JMultipleOsdInfo;
import com.hankvision.ipcsdk.JOSDInfo;
import com.hankvision.ipcsdk.JTimeOsdInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements Dllipcsdk.CBRawData
{
    static {
        System.loadLibrary("native-lib");
    }
    int oldCam = 1;
    String strIp = "192.168.1.18";
    String strUsername = "admin";
    String strPassword= "admin";
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
    private byte[] sps= {0,0,0,1,103,77,0,30,-107,-88,40,11,-2,89,-72,8,8,8,16};
    private byte[] sps_hd={0,0,0,1,103,77,0,31,-107,-88,20,1,110,-101,-128,-128,-128,-127};
    private byte[] cur_sps;
    private byte[] pps= {0,0,0,1,104,-18,60,-128/*,0,0,0,1,6,-27,1,91,-128*/};

    private Thread t1;
    SurfaceView surfaceViewDecode; // 视频播放绑定的surface
    MediaCodecDecoder mediaCodecDecoder; //解码器
    public static BlockingDeque<BufferInfo> bq;
    public BlockingDeque<BufferInfo> xbq;
    public IoCtrl ioCtrl;
    private Button buttonConnect;
    //private ImageView imageViewLed2;
    public  Handler handler;
    private ImageView imageViewSettings;
    //private ImageButton ButtonZoomTele;
    //private ImageButton ButtonZoomWide;
   // private ImageButton ButtonMotorUp;
    //private ImageButton ButtonMotorDown;
    private RadioButton radioButtonCamType;
    private TextView osdInfoTx;
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.menu,menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.settings:
                noUseToast();
                //Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                //startActivityForResult(intent,0x100);

                break;
            case R.id.regard:
                //Intent intent1 = new Intent(MainActivity.this, Regard.class);
                //startActivity(intent1);
                System.exit(1);
                noUseToast();
                break;
        }
        return true;
    }

    /** Called when the activity is first created. */
    @Override


    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewSettings = (ImageView) findViewById(R.id.imageSettings);
        registerForContextMenu(imageViewSettings);
        buttonConnect = findViewById(R.id.button);
        //imageViewLed2 = (ImageView) findViewById(R.id.buttonLED2);
        //ButtonZoomTele = (ImageButton) findViewById(R.id.imageViewZOOM_TELE);
        //ButtonZoomWide = (ImageButton) findViewById(R.id.imageViewZOOM_WIDE);
       // ButtonMotorUp = (ImageButton) findViewById(R.id.imageViewMotorUp);;
       // ButtonMotorDown = (ImageButton) findViewById(R.id.imageViewMotorDown);
        radioButtonCamType = (RadioButton) findViewById(R.id.radioButton);
        osdInfoTx=findViewById(R.id.osdInfo);

        //添加菜单
        initVideoList();

       /* ButtonZoomTele.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean bRet;

                if(event.getAction()==MotionEvent.ACTION_DOWN)
                {
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_LENS_ZOOM_IN,(byte)1);
                    //bRet = Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_TELE.ordinal(), 6, 6, false);
                }else if(event.getAction()==MotionEvent.ACTION_UP)
                {
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_PTZ_STOP,(byte)0);
                    //Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_TELE.ordinal(), 6, 6, true);
                    AVAPIsClient.setOSD();
                }
                return false;
            }
        });

        ButtonZoomWide.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean bRet;
                if(event.getAction()==MotionEvent.ACTION_DOWN)
                {
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_LENS_ZOOM_OUT,(byte)1);
                    //bRet = Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_WIDE.ordinal(), 6, 6, false);
                }else if(event.getAction()==MotionEvent.ACTION_UP)
                {
                    AVAPIsClient.ctrlPTZ(AVAPIsClient.AVIOCTRL_PTZ_STOP,(byte)0);
                    //Dllipcsdk.IPCNET_PTZControl(strIp, nVideoPort, Dllipcsdk.E_PTZ_COMMAND.ZOOM_WIDE.ordinal(), 6, 6, true);

                }
                return false;
            }
        });*/

        byte[] buffer = new byte[4];
        buffer[0] = 0x0a;
        buffer[3] = (byte) 0xe0;
        buffer[1] = 0x02;
       /* ButtonMotorUp.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN)
                {
                    buffer[2] = 1;

                    TaskCenter.sharedCenter().send(buffer);
                }else if(event.getAction()==MotionEvent.ACTION_UP)
                {
                    buffer[2] = 0;
                    TaskCenter.sharedCenter().send(buffer);
                }
                return false;
            }
        });


        ButtonMotorDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN)
                {
                    buffer[2] = 2;

                    TaskCenter.sharedCenter().send(buffer);
                }else if(event.getAction()==MotionEvent.ACTION_UP)
                {
                    buffer[2] = 0;
                    TaskCenter.sharedCenter().send(buffer);
                }
                return false;
            }
        });*/
        SetParam(osdInfoTx);
    }

    //辅助灯
    private boolean led2=false;
    public  void onClickOpenLed2(View view){
        byte[] buffer = new byte[4];
        buffer[0] = 0x0a;
        buffer[3] = (byte) 0xe0;
        buffer[1] = 0x04;
        if(led2==true){
            buffer[2] = 0;
            led2 = false;
            //imageViewLed2.setImageResource(R.drawable.deng4_1);
            VideoThread.startReceive = false;
        } else{
          buffer[2] = 1;
            //imageViewLed2.setImageResource(R.drawable.deng4);
          led2 = true;
          VideoThread.startReceive = true;
        }
        TaskCenter.sharedCenter().send(buffer);
        //Toast.makeText(MainActivity.this,"指令下达",Toast.LENGTH_SHORT).show();
    }


    /**
     *  链接摄像头
     */
    int camConnect = 0;
    public void connectIPC() {
        //--------------------------------------- 解码 ------------------------------------
        // 此线程从阻塞队列poll buffer信息并送入解码器
        t1 =new Thread(() -> {
            BufferInfo temp;
            while(true){
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Java技术栈线程被中断，程序退出。");
                    return;
                }
                try{
                    temp=bq.poll(3000, TimeUnit.MILLISECONDS);
                    if(temp==null) { continue;}
                    // 向解码器输入buffer

                    mediaCodecDecoder.input(temp.buffer,temp.len,System.nanoTime()/1000);
                    mediaCodecDecoder.output();
                }catch (Exception e){
//                    e.printStackTrace();
                }
            }
        });
        t1.start();

        // 此线程从阻塞队列poll buffer信息并送入解码
        // 绑定surfaceview
        surfaceViewDecode= findViewById(R.id.surfaceView);
        //实例化解码器
        mediaCodecDecoder=new MediaCodecDecoder();
        // 初始化
        try{
            mediaCodecDecoder.init();
        } catch (Exception e){
            e.printStackTrace();
        }

        // surfaceView绘制完成后 配置解码器并启动
        surfaceViewDecode.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                // 配置解码器
                mediaCodecDecoder.configure(surfaceViewDecode.getHolder().getSurface());
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
        });

        mediaCodecDecoder.configure(surfaceViewDecode.getHolder().getSurface());
        System.out.println("配置解码器");
        // 启动解码器
        mediaCodecDecoder.start();

        camConnect = 1;
        buttonConnect.setText("已链接");
        buttonConnect.setBackgroundColor(0xff00ff00);
        SetParam(osdInfoTx);
    }


    public void onClickOpen(View view) throws IOException {

        if(camConnect == 1) {
            Toast.makeText(MainActivity.this, "摄像机已经链接", Toast.LENGTH_SHORT).show();
            return;
        }
        connectIPC();
        //buttonConnect.setBackgroundColor(0xff0320);
        camConnect = 1;
    }

    public void MediaCodecDecoder(){
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
    public int output(){
        mBI = new MediaCodec.BufferInfo();
        int i = mMC.dequeueOutputBuffer(mBI, oBUFFER_TIMEOUT);// 把转码后的数据存到mBI
//        Log.i("output","index:"+i);

        while(i >= 0){
            ByteBuffer outputBuffer =mMC.getOutputBuffers()[i];
            /**
             * 获取输出数据
             * 第二个参数设置为true，表示解码显示在Surface上
             */
            mMC.releaseOutputBuffer(i, true);
            i = mMC.dequeueOutputBuffer(mBI, oBUFFER_TIMEOUT);
        }
        return BUFFER_OK;
    }

    public void flush(){
        mMC.flush();
    }

    public void release() {
        flush();
        mMC.stop();
        mMC.release();
//        mMC = null;
        Log.d("release","successful release");
    }

    /**
     * 保存参数
     */
    private void saveUserInfo(){
        SharedPreferences   userInfo = getSharedPreferences(UID,MODE_PRIVATE);
        SharedPreferences.Editor editor = userInfo.edit();
        System.out.print("saveUserInfo\n");
        editor.putString("name","admin");
        editor.putString("key","123456");
        editor.putString("uid","41R82E1J52USSEHZ111A");
        editor.apply();
    }

    private void getUserInfo(){
        SharedPreferences userInfo = getSharedPreferences(UID,MODE_PRIVATE);
        System.out.print("getUserInfo\n");
        String name;
        name = userInfo.getString("name",null);
        System.out.println(name);
    }

    private void noUseToast(){
        Toast.makeText(MainActivity.this,"暂无功能",Toast.LENGTH_SHORT).show();
    }

    /**
     * 初始化保存的视频列表
     */
    private void initVideoList() {
        //videoInfoArrayList.clear();
        //File file = new File(Environment.getDataDirectory().getPath()+ "/ACAM_VIDEOS");
        //if (!file.exists()) {
            //file.mkdirs();
            //System.out.println("file.mkdirs");
        //}
        //String[] fileNames = file.list();
        //File[] filePaths = file.listFiles();
            //for (int i = 0; i < fileNames.length; i++) {
            //VideoInfo videoInfo = new VideoInfo();
            //videoInfo.setVideoName(fileNames[i]);
            //videoInfo.setVideoPath(filePaths[i]);
            //videoInfoArrayList.add(videoInfo);
            //}
//        System.out.println("videoInfoArrayList.size(): " + videoInfoArrayList.size());
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

        if(nErrorType == 0) {
            String str = String.format("%x %d %d %d %d %d %d %d %d %d %d %d %d\n", lRawHandle, lRawBufSize, m_AVType, m_EncoderType, m_FrameType,
                    m_FrameRate, m_VideoWidth, m_VideoHeight, m_Channels, m_Samples, m_BitCount, m_TimeStamp, lRawBufSize);

            //System.out.println(str);

            // 在此处接收码流
        }
        else if (nErrorType < 0)
        {
            String str;
            if (nErrorType == -1000) {
                str = String.format("接设备失败:%d_%d\n", nErrorType, nErrorCode);
            }
            else if (nErrorType == -999){
                str = String.format("登录设备失败，用户名或密码错误:%d_%d\n", nErrorType, nErrorCode);
            }
            else if (nErrorType == -998){
                str = String.format("请求视频失败，一般为设备原因:%d_%d\n", nErrorType, nErrorCode);
            }
            else {
                str = String.format("其它错误:%d_%d\n", nErrorType, nErrorCode);
            }
        }
    }

    long lRawHandle = -1;

    public void StartRaw(View v) {
        if(radioButtonCamType.isChecked()){
            oldCam = 0;
        }
        ioCtrl.initTCP();
        if(oldCam==0) {
            getUserInfo();
            saveUserInfo();

            bq = new LinkedBlockingDeque<>();// videobuffer信息存储到这里 解码器从此阻塞队列poll video的信息
            (new Thread() {
                public void run() {
                    int stu = AVAPIsClient.start(MainActivity.this.UID, bq);
                    System.out.println("x连接线程中断++++++");
                }
            }).start();
            connectIPC();
            return;
        }
        System.out.println("StartRaw");
        if (lRawHandle == -1) {

            lRawHandle = Dllipcsdk.IPCNET_StartRawData(strIp, nVideoPort, 0, strUsername, strPassword, 1, this);
        }
        if(lRawHandle != -1) {
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

        if (lPlay == -1)
        {
            SurfaceView surfaceView = findViewById(R.id.surfaceView);
            if (surfaceView != null) {
                lPlay = Dllipcsdk.IPCNET_StartRawPlay(strIp, nVideoPort, 0, "admin", "admin", 1, surfaceView.getHolder().getSurface());
            }
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
        Log.i("SetParam","开始获取OSD");
        JOSDInfo josdInfo = Dllipcsdk.IPCNET_GetOsdInfo(strIp, nHttpPort, 0, strUsername, strPassword);
        if (josdInfo != null) {
            Log.i("SetParam","josdInfo:  ----- " + josdInfo.TextOSDTitle);
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

        if (lRecord == -1)
        {
            int lcurrentTimeMillis = Integer.parseInt(String.valueOf(System.currentTimeMillis() / 1000));

            String mp4FilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM"+ File.separator + "Video_" + lcurrentTimeMillis + ".mp4";

            lRecord = Dllipcsdk.IPCNET_StartRecord(strIp, nVideoPort, mp4FilePath, 0);
        }
    }

    public void StopRecord(View v) {

        if (lRecord != -1) {

            Dllipcsdk.IPCNET_StopRecord(lRecord);

            lRecord = -1;
        }
    }

}
