package com.camera.model.api;


import android.content.Context;
import android.util.Log;
import android.view.Surface;

import com.camera.model.SaveFrames;
import com.decode.EncoderVideoRunnable;
import com.decode.MediaMuxerUtils;
import com.decode.tools.BufferInfo;
import com.hankvision.ipcsdk.Dllipcsdk;
import com.hankvision.ipcsdk.JOSDInfo;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.St_SInfo;
import com.zhuangliming.camok.IoCtrl;
import com.zhuangliming.camok.OsdSharePreference;
import com.zhuangliming.camok.model.MessageEvent;


import org.greenrobot.eventbus.EventBus;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class AVAPIsClient {
    private static int sid; // tutk_platform_free session ID
    static  String strIp = "192.168.1.18";
    static String strUsername = "admin";
    static String strPassword = "admin";
    private static String defaultUid="41R82E1J52USSEHZ111A"; // 摄像头的uid
    private static String defaultUsername = "admin";
    private static String defaultPassword = "123456";
    private static int avIndex = -1; // avClientStart的返回值
    private static Thread audioThread;
    private static Thread videoThread;
    public static boolean isStarted = false;
    public static IoCtrl ioCtrl;
    /**
     * 修改视频清晰度的常量
     */
    public static byte nowQuality = -1;
    public static final byte AVIOCTRL_QUALITY_HIGH = 0x02;  // 640*480, 10fps, 256kbps  超清
    public static final byte AVIOCTRL_QUALITY_MIDDLE = 0x03;// 320*240, 15fps, 256kbps  高清
    public static final byte AVIOCTRL_QUALITY_LOW = 0x04;   // 320*240, 10fps, 128kbps  标清
    public static final byte AVIOCTRL_QUALITY_MIN = 0x05;   // 160*120, 10fps, 64kbps   流畅
    private static final int IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ = 0x0320;

    private static final int IOTYPE_USER_IPCAM_PTZ_COMMAND = 0x1001;
    public static final byte AVIOCTRL_PTZ_STOP             = 0;
    public static final byte AVIOCTRL_LENS_ZOOM_IN        = 23;
    public static final byte AVIOCTRL_LENS_ZOOM_OUT       = 24;

    private static final int IOTYPE_USER_IPCAM_GET_TIMEZONE_REQ =(0x3A0);
    private static final int IOTYPE_USER_IPCAM_GET_TIMEZONE_RESP =(0x3A1);
    private static final int IOTYPE_USER_IPCAM_SET_TIMEZONE_REQ =(0x3B0);
    private static final int IOTYPE_USER_IPCAM_SET_TIMEZONE_RESP =(0x3B1);
    /**
     * 开始连接设备
     */
    private static Context mContext;
    public static MediaMuxerUtils mMuxerUtils ;
    private static EncoderVideoRunnable videoRunnable;
    public static int start(Context context, Surface surface) {
        mContext=context.getApplicationContext();
        /*videoRunnable=new EncoderVideoRunnable(null,surface);
        new Thread(videoRunnable).start();*/
        mMuxerUtils =MediaMuxerUtils.getMuxerRunnableInstance(surface);
        mMuxerUtils.startDecode();
        //mMuxerUtils.startMuxerThread();
        //username = user.getUsername();
        //password = user.getPassword();
        //AVAPIsClient.uid =user.getUID();
        System.out.println("开始连接...");
        String username=OsdSharePreference.getInstance(mContext.getApplicationContext()).getString("username",defaultUsername);
        String password=OsdSharePreference.getInstance(mContext.getApplicationContext()).getString("password",defaultPassword);
        String uid=OsdSharePreference.getInstance(mContext.getApplicationContext()).getString("uid",defaultUid);

        Log.i("连接信息","用户名："+username+"密码："+password+"uid:"+uid);
        // 初始化IOTC(物联网)端，需在调用任何IOTC相关函数前调用次函数,此函数利用ip连接主机
        // 参数0代表随机选取UDP端口
        // 初始化成功返回常量 IOTC_ER_NoERROR
        int ret = IOTCAPIs.IOTC_Initialize2(0);
        System.out.println("IOTC_Initialize2 return = " + ret);
        if(ret != IOTCAPIs.IOTC_ER_NoERROR) {
            System.out.println("初始化失败...IOTCAPIs_Device可能已经存在");
            return -1;
        }

        // 初始化 AV 模块
        // 调用 AV 模块函数前，必须初始化
        // 参数为 AV频道的最大数目
        AVAPIs.avInitialize(3);
        sid = IOTCAPIs.IOTC_Get_SessionID();

        if(sid < 0) {
            System.out.printf("IOTC_Get_SessionID error code [%d]\n", sid);
            return -2;
        }

        // 客户端将设备uid和tutk_platform_free session ID绑定,从而在物联网端连接设备
        ret = IOTCAPIs.IOTC_Connect_ByUID_Parallel(uid, sid);
        System.out.println("IOTC_Connect_ByUID_Parallel ret = " + ret);
        St_SInfo info=new St_SInfo();
        IOTCAPIs.IOTC_Session_Check(sid,info);
        int ipLen=returnActualLength(info.RemoteIP);
        byte[] tempIp = new byte[ipLen];
        System.arraycopy(info.RemoteIP,0,tempIp,0,ipLen);
        String ip = new String(tempIp);
            Log.i("ip","ip地址："+ip);
           Log.i("ip","ip端口："+info.RemotePort);
        //ioCtrl.initTCP("192.168.1.4",8234);
        ioCtrl.initTCP(ip,8100);
        int[] servType = new int[1];
        // 接收AV数据前应通过AV服务器的认证
        avIndex = AVAPIs.avClientStart(sid, username, password, 20000, servType, 0);
        AVAPIsClient.avIndex = avIndex;
        if (avIndex < 0) {
            Log.i("Decode","连接失败");
            //System.out.printf("avClientStart 连接失败[%d]\n", avIndex);
            return -3;
        } else {
            Log.i("Decode","连接成功");
            EventBus.getDefault().post(new MessageEvent(MessageEvent.CONNECT_SUCCESS,null));
        }
        if (startIpcamStream(avIndex)) {
            //getOSD();
            System.out.println("startVideoThread");
            startVideoThread();
            videoThread.start();
            isStarted=true;
            /*try {
                videoThread.join();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                return -4;
            }*/
        }
        return 0;
    }

    public static void stopDecode(){
        //mMuxerUtils.stopMuxer();
       mMuxerUtils.exit();
    }
    public static void releaseDecodec(){
        //mMuxerUtils.stopMuxer();
        mMuxerUtils.releaseDecodec();
    }
    public static int returnActualLength(byte[] data) {
        int i = 0;
        for (; i < data.length; i++) {
            if (data[i] == '\0')
                break;
        }
        return i;
    }
    public static boolean isRecording(){
        return mMuxerUtils.isMuxerStarted();
    }
    // 用来判断是否和服务器建立了 IO 连接
    public static boolean startIpcamStream(int avIndex) {
        AVAPIs av = new AVAPIs();
        // 手机向服务端发送 IO 控制
        int ret = av.avSendIOCtrl(avIndex, AVAPIs.IOTYPE_INNER_SND_DATA_DELAY,
                new byte[2], 2);
        if (ret < 0) {
            System.out.printf("start_ipcam_stream failed[%d]\n", ret);

            return false;
        }

        // This IOTYPE constant and its corrsponsing data structure is defined in
        // Sample/Linux/Sample_AVAPIs/AVIOCTRLDEFs.h
        //
        int IOTYPE_USER_IPCAM_START = 0x1FF;
        ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START,
                new byte[8], 8);
        if (ret < 0) {
            System.out.printf("start_ipcam_stream failed[%d]\n", ret);
            return false;
        }

        int IOTYPE_USER_IPCAM_AUDIOSTART = 0x300;
        ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_AUDIOSTART,
                new byte[8], 8);
        if (ret < 0) {
            System.out.printf("start_ipcam_stream failed[%d]\n", ret);
            return false;
        }

        return true;
    }

/*    public static void setOSD(){
        JOSDInfo josdInfo = Dllipcsdk.IPCNET_GetOsdInfo(strIp, 80, avIndex, strUsername, strPassword);
        Log.i("SetParam", "开始获取OSD"+josdInfo==null?"没获取到":"获取到了");
        if (josdInfo != null) {
            Log.i("SetParam", "josdInfo:  ----- " + josdInfo.TextOSDTitle);
            //System.out.println("josdInfo:  ----- " + josdInfo.TextOSDTitle);

            // Dllipcsdk.IPCNET_SetOsdInfo(strIp, nHttpPort, 0, josdInfo, strUsername, strPassword);
        }
    }*/

    public static void getOSD(){
        JOSDInfo josdInfo = Dllipcsdk.IPCNET_GetOsdInfo(strIp, 80, avIndex, strUsername, strPassword);
        Log.i("SetParam", "开始获取OSD"+josdInfo==null?"没获取到":"获取到了");
        if (josdInfo != null) {
            Log.i("SetParam", "josdInfo:  ----- " + josdInfo.TextOSDTitle);
            //System.out.println("josdInfo:  ----- " + josdInfo.TextOSDTitle);

            // Dllipcsdk.IPCNET_SetOsdInfo(strIp, nHttpPort, 0, josdInfo, strUsername, strPassword);
        }
    }
    public static void startVideoThread() {
        if (startIpcamStream(avIndex)) {
            videoThread = new Thread(new VideoThread(avIndex),
                   "Video-Thread");
            //videoThread.start();
        }
    }




    public static void controlVideoThread() {
        videoThread.interrupt();
    }

    public static void startAudioThread() {
        if (startIpcamStream(avIndex)) {
            //audioThread = new Thread(new AudioThread(avIndex),
           //         "Audio-Thread");
        }
    }

    public static void controlAudioThread(int flag) {
        if (flag == 0) {
            //audioThread.start();
        } else {
           // audioThread.interrupt();
        }
    }
    public static class SMsgAVIoctrlSetStreamCtrlReq {
        int channel; // Camera Index
        byte quality; // AVIOCTRL_QUALITY_XXXX
        byte[] reserved = new byte[3];

        public static byte[] parseContent(int channel, byte quality) {

            byte[] result = new byte[8];
            //byte[] ch = Packet.intToByteArray_Little(channel);
            //System.arraycopy(ch, 0, result, 0, 4);
            result[4] = quality;

            return result;
        }
    }
    /**
     * 修改视频清晰度
     * @param qualityNum
     * qualityNum 0 - 3 依次为 流畅 吧标清 高清 超清
     */
    public static void setQuality(int qualityNum) {


        AVAPIs av = new AVAPIs();
        switch (qualityNum) {
            // 流畅
            case 0:
                if(nowQuality != AVIOCTRL_QUALITY_MIN) {
                    System.out.println("视频切换为流畅");
                    byte[] result = SMsgAVIoctrlSetStreamCtrlReq.parseContent(avIndex, AVIOCTRL_QUALITY_MIN);
                    int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ,
                            result, 8);
                    if (ret < 0) {
                        System.out.printf("切换流畅失败 [%d]\n", ret);
                    }
                    nowQuality = AVIOCTRL_QUALITY_MIN;
                }

                break;
            // 标清
            case 1:
                if(nowQuality != AVIOCTRL_QUALITY_LOW) {
                    System.out.println("视频切换为标清");
                    byte[] result = SMsgAVIoctrlSetStreamCtrlReq.parseContent(avIndex, AVIOCTRL_QUALITY_LOW);
                    int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ,
                            result, 8);
                    if (ret < 0) {
                        System.out.printf("切换标清失败 [%d]\n", ret);
                    }
                    nowQuality = AVIOCTRL_QUALITY_LOW;
                }
                break;
                // 高清
            case 2:
                if(nowQuality != AVIOCTRL_QUALITY_MIDDLE) {
                    System.out.println("视频切换为高清");
                    byte[] result = SMsgAVIoctrlSetStreamCtrlReq.parseContent(avIndex, AVIOCTRL_QUALITY_MIDDLE);
                    int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ,
                            result, 8);
                    if (ret < 0) {
                        System.out.printf("切换高清失败 [%d]\n", ret);
                    }
                    nowQuality = AVIOCTRL_QUALITY_MIDDLE;
                }
                break;
            case 3:
                if(nowQuality != AVIOCTRL_QUALITY_HIGH) {
                    System.out.println("视频切换为超清");
                    byte[] result = SMsgAVIoctrlSetStreamCtrlReq.parseContent(avIndex, AVIOCTRL_QUALITY_HIGH);
                    int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_SETSTREAMCTRL_REQ,
                            result, 8);
                    if (ret < 0) {
                        System.out.printf("切换超清失败 [%d]\n", ret);
                    }
                    nowQuality = AVIOCTRL_QUALITY_HIGH;
                }
                break;
                default:
                    System.out.println("切换失败");
                    break;
        }
    }

    public static void ctrlPTZ(byte cmd,byte speed)
    {
        AVAPIs av = new AVAPIs();
        byte[] result = new byte[8];
        result[0] = cmd;//AVIOCTRL_LENS_ZOOM_OUT;
        result[1] = speed;
        result[5] = 0;//channel
        int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_PTZ_COMMAND ,result,8);
    }

    public  static void setOSD(){
        AVAPIs av = new AVAPIs();
        byte[] result = new byte[268];
        result[0] = 268>>8;//AVIOCTRL_LENS_ZOOM_OUT;
        result[1] = 268&0xff;
        result[5] = 0;//channel
        int ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_SET_TIMEZONE_RESP   ,result,268);
        Log.i("setOSD结果",ret+"");
    }

    public  static void setOSD1(){

        ioCtrl.writeOsd();
       // Log.i("setOSD结果",ret+"");
    }
    /**
     * 关闭连接
     */
    public static void close() {
        Log.i("AVAPIsClient","关闭连接");
        AVAPIs.avClientStop(avIndex);
        System.out.println("avClientStop OK");
        IOTCAPIs.IOTC_Session_Close(sid);
        System.out.println("IOTC_Session_Close OK");
        AVAPIs.avDeInitialize();
        IOTCAPIs.IOTC_DeInitialize();
        System.out.printf("StreamClient exit...\n");
    }



    public static byte[] convertG711ToPCM(byte[] g711Buffer, int length, byte[] pcmBuffer) {
        System.out.println("g711Buf:" + Arrays.toString(g711Buffer));
        if (pcmBuffer == null) {
            pcmBuffer = new byte[length * 2];
        }
        for (int i = 0; i < length; i++) {
            byte alaw = g711Buffer[i];
            alaw ^= 0xD5;

            int sign = alaw & 0x80;
            int exponent = (alaw & 0x70) >> 4;
            int value = (alaw & 0x0F) >> 4 + 8;
            if (exponent != 0) {
                value += 0x0100;
            }
            if (exponent > 1) {
                value <<= (exponent - 1);
            }
            value = (char) ((sign == 0 ? value : -value) & 0xFFFF);
            pcmBuffer[i * 2] = (byte) (value & 0xFF);
            pcmBuffer[i * 2 + 1] = (byte) (value >> 8 & 0xFF);
        }
        System.out.println("PCM:" + Arrays.toString(pcmBuffer));
        return pcmBuffer;
    }

    public static class VideoThread implements Runnable {
        static int VIDEO_BUF_SIZE = 150000;
        static final int FRAME_INFO_SIZE = 16;

        private int avIndex;
        public VideoThread(int avIndex) {
            this.avIndex = avIndex;
        }

        @Override
        public void run() {
            System.out.printf("[%s] Start\n",
                    Thread.currentThread().getName());
            Log.i("Decode","VideoThread启动，获取视频流");
            AVAPIs av = new AVAPIs();
            byte[] frameInfo = new byte[FRAME_INFO_SIZE];
            int[] outBufSize = new int[1];
            int[] outFrameSize = new int[1];
            int[] outFrmInfoBufSize = new int [1];
            //SaveFrames saveFrames = new SaveFrames();
            byte[] videoBuffer;
            long index=0;
            while (true) {
                if(mMuxerUtils.isExit){
                    return;
                }
                videoBuffer = new byte[VIDEO_BUF_SIZE];
                int[] frameNumber = new int[1];
                int ret = av.avRecvFrameData2(avIndex, videoBuffer,
                        VIDEO_BUF_SIZE, outBufSize, outFrameSize,
                        frameInfo, FRAME_INFO_SIZE,
                        outFrmInfoBufSize, frameNumber);
                Log.i("视频流结果",ret+"");
                if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
                    try {
                       // VIDEO_BUF_SIZE=VIDEO_BUF_SIZE-50000;
                       // videoBuffer=new byte[VIDEO_BUF_SIZE];
                        Thread.sleep(30);
                        continue;
                    }
                    catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                }
                else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
                    System.out.printf("[%s] Lost video frame number[%d]\n",
                            Thread.currentThread().getName(), frameNumber[0]);
                    continue;
                }
                else if (ret == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
                    System.out.printf("[%s] Incomplete video frame number[%d]\n",
                            Thread.currentThread().getName(), frameNumber[0]);
                    continue;
                }
                else if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
                    System.out.printf("[%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
                            Thread.currentThread().getName());
                    break;
                }
                else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                    System.out.printf("[%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
                            Thread.currentThread().getName());
                    break;
                }
                else if (ret == AVAPIs.AV_ER_INVALID_SID) {
                    System.out.printf("[%s] Session cant be used anymore\n",
                            Thread.currentThread().getName());
                    break;
                }
                Log.i("Decode","Video实际长度"+outFrameSize[0]);
                // Now the data is ready in videoBuffer[0 ... ret - 1]
                // Do something here
                //sendFrame(new BufferInfo(outFrameSize[0], videoBuffer));
                /*if(videoRunnable != null){
                    videoRunnable.addData(videoBuffer);
                }*/
                mMuxerUtils.addVideoFrameData(new BufferInfo(outFrameSize[0], videoBuffer,index++));
                //---------------------------------------------------------------------
                /*if (startReceive) {
                    saveFrames.saveFrames(videoBuffer, frameInfo, ret);
                } else {
                    saveFrames.stopReceive();
                }*/
            }

            System.out.printf("[%s] Exit\n",
                    Thread.currentThread().getName());
        }
    }

    public static BlockingDeque blockingDeque=new LinkedBlockingDeque();
    public static BufferInfo readFrame()  {
        Log.i("Decode","buffer出队");
        try {
            return (BufferInfo) blockingDeque.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void sendFrame(BufferInfo bi){
        Log.i("Decode","buffer入队");
        blockingDeque.offer(bi);
    }


}

