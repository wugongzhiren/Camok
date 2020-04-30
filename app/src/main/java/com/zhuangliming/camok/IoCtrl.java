package com.zhuangliming.camok;


import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.zhuangliming.camok.video.PollingUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import static android.os.Looper.getMainLooper;

public class IoCtrl {

    public final byte MOTOR_STOP = 0;
    public final byte MOTOR_UP = 1;
    public final byte MOTOR_DOWN = 2;
    private Context mContext;
    //public static Random rand = new Random();
    //public static Runnable runnable;
    public static IoCtrl instance;

    //    提供一个全局的静态方法
    public static IoCtrl getInstance() {
        if (instance == null) {
            synchronized (IoCtrl.class) {
                if (instance == null) {
                    instance = new IoCtrl();
                }
            }
        }
        return instance;
    }

    public void initTCP(String ip, int port, Context context) {
        mContext = context.getApplicationContext();
        TaskCenter.sharedCenter().connect(ip, port);
        TaskCenter.sharedCenter().setDisconnectedCallback(new TaskCenter.OnServerDisconnectedCallbackBlock() {
            @Override
            public void callback(IOException e) {

            }
        });
        TaskCenter.sharedCenter().setConnectedCallback(new TaskCenter.OnServerConnectedCallbackBlock() {
            @Override
            public void callback() {
                Log.i("TCP_IO", "连接上了TCP");
            }
        });
        TaskCenter.sharedCenter().setReceivedCallback(new TaskCenter.OnReceiveCallbackBlock() {
            @Override
            public void callback(String receicedMessage) {
                Log.i("TCP_IO", "接收到了信息：" + receicedMessage);
            }
        });
    }

    public void writeMotor(int value) {
        byte[] buffer = new byte[4];
        buffer[0] = 0x0a;
        buffer[1] = 0x02;
        buffer[2] = (byte) value;
        buffer[3] = (byte) 0xe0;
        TaskCenter.sharedCenter().send(buffer);
    }

    /*public void openOsd() {
        byte[] data = new byte[1024];
        //head 头，固定
        data[6] = 0x11;
        data[7] = (byte) 0xa8;
        data[8] = 0x01;
        data[9] = (byte) 0xa4;
        //uint8_t heard[10];
        //uint8_t bFlag;      // 0-不显示，1-要显示
        //uint8_t	bMaxLine;   // 最多要显示的行数  1-12 ，其它值默认为6
        // uint8_t	bMaxNum;    // 每行最多需要显示的字符数  1-64，其它值默认为32
        //uint8_t	bFont ;     // 字体大小，1-16*16，2-32*32，3-48*48，4-64*64，其它值为自动
        // uint8_t bSave;	    // 0-动态显示，1-保存显示
        //  uint8_t bChange;    // 1-数据有修改，需要显示刷新， 内部用
        // uint8_t	bBlank[6];
        // TextType text[16];

        //typedef struct {
        //  uint8_t  bFlag;   // 0-不显示，1-要显示，2-要显示并且数据有改动
        //  uint8_t  bXPos;
        // uint8_t	 bYPos;
        //   uint8_t	 bColor;   // 色彩
        //   uint8_t  text[64]; // 需要显示字符
        // }TextType;
        //bFlag;      // 0-不显示，1-要显示
        data[10] = 1;
        TaskCenter.sharedCenter().send(data);
    }*/

    public void closeOsd() {
        if (runnable != null && pollingUtil != null) {
            pollingUtil.endPolling(runnable);
            Log.i("OSD", "先停止定时任务");
        }
        byte[] data = new byte[1024];
        //head 头，固定
        data[6] = 0x11;
        data[7] = (byte) 0xa8;
        data[8] = 0x01;
        data[9] = (byte) 0xa4;
        data[10] = 0;
        TaskCenter.sharedCenter().send(data);
    }

    public void showOsd() {
        byte[] data = new byte[1024];
        data[6] = 0x11;
        data[7] = (byte) 0xa8;
        data[8] = 0x01;
        data[9] = (byte) 0xa4;
        data[10] = 1;// 0-不显示，1-要显示
        data[11] = 6;// 最多要显示的行数  1-12 ，其它值默认为6
        data[12] = 32;// 每行最多需要显示的字符数  1-64，其它值默认为32
        data[13] = 2;// 字体大小，1-16*16，2-32*32，3-48*48，4-64*64，其它值为自动
        data[14] = 1;// 0-动态显示，1-保存显示
        data[15] = 1;// 1-数据有修改，需要显示刷新， 内部用
        //中间保留6个字节
        //文字部分，目前是4行显示，除去文字信息，还能使用64个字节
        //byte[] tasknameB;
        try {
            //写入第一行信息
            String taskname = OsdSharePreference.getInstance(mContext).getString("taskname");
            taskname = "检测任务:" + taskname;
            data[22] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
            data[23] = 0;//bXPos
            data[24] = 1;//bYPos;
            data[25] = 0;//bColor;   // 色彩
            byte[] tasknameB = taskname.getBytes("GBK");
            System.arraycopy(tasknameB, 0, data, 26, tasknameB.length);
            data[26 + tasknameB.length] = '\0';
            //写入第二行信息
            data[90] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
            data[91] = 0;//bXPos
            data[92] = 2;//bYPos;
            data[93] = 0;//bColor;   // 色彩
            String wellname = OsdSharePreference.getInstance(mContext).getString("wellname");
            wellname = "井号信息:" + wellname;
            byte[] wellnameB = wellname.getBytes("GBK");
            System.arraycopy(wellnameB, 0, data, 94, wellnameB.length);
            data[94 + wellnameB.length] = '\0';
            //写入第三行信息
            data[158] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
            data[159] = 0;//bXPos
            data[160] = 3;//bYPos;
            data[161] = 0;//bColor;   // 色彩
            String checkinfo = OsdSharePreference.getInstance(mContext).getString("checkinfo");
            checkinfo = "检测信息:" + checkinfo;
            byte[] checkinfoB = checkinfo.getBytes("GBK");
            System.arraycopy(checkinfoB, 0, data, 162, checkinfoB.length);
            data[162 + checkinfoB.length] = '\0';
            //写入第4行信息
            data[226] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
            data[227] = 0;//bXPos
            data[228] = 4;//bYPos;
            data[229] = 0;//bColor;   // 色彩
            String checkcompany = OsdSharePreference.getInstance(mContext).getString("checkcompany");
            checkcompany = "检测单位:" + checkcompany;
            byte[] checkcompanyB = checkcompany.getBytes("GBK");
            System.arraycopy(checkcompanyB, 0, data, 230, checkcompanyB.length);
            data[230 + checkcompanyB.length] = '\0';
            /*String text = "测试文字";
            byte[] textB = text.getBytes("GBK");
            Log.i("文字", textB.length + "");
            System.arraycopy(textB, 0, data, 26, textB.length);
            data[34] = '\0';*/
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        TaskCenter.sharedCenter().send(data);
    }

    public static void writeOsd() {
        byte[] data = new byte[1024];
        data[6] = 0x11;
        data[7] = (byte) 0xa8;
        data[8] = 0x01;
        data[9] = (byte) 0xa4;
        //uint8_t heard[10];
        //uint8_t bFlag;      // 0-不显示，1-要显示
        //uint8_t	bMaxLine;   // 最多要显示的行数  1-12 ，其它值默认为6
        // uint8_t	bMaxNum;    // 每行最多需要显示的字符数  1-64，其它值默认为32
        //uint8_t	bFont ;     // 字体大小，1-16*16，2-32*32，3-48*48，4-64*64，其它值为自动
        // uint8_t bSave;	    // 0-动态显示，1-保存显示
        //  uint8_t bChange;    // 1-数据有修改，需要显示刷新， 内部用
        // uint8_t	bBlank[6];
        // TextType text[16];

        //typedef struct {
        //  uint8_t  bFlag;   // 0-不显示，1-要显示，2-要显示并且数据有改动
        //  uint8_t  bXPos;
        // uint8_t	 bYPos;
        //   uint8_t	 bColor;   // 色彩
        //   uint8_t  text[64]; // 需要显示字符
        // }TextType;
        data[10] = 1;
        //System.arraycopy(intToBytes(1),0,data,10,4);
        data[11] = 6;
        //System.arraycopy(intToBytes(6),0,data,14,4);
        data[12] = 32;
        //System.arraycopy(intToBytes(32),0,data,18,4);
        data[13] = 2;
        //System.arraycopy(intToBytes(2),0,data,22,4);
        data[14] = 1;
        //System.arraycopy(intToBytes(1),0,data,26,4);
        data[15] = 1;
        //System.arraycopy(intToBytes(1),0,data,30,4);
        //中间略过6个字节
        //文字部分
        data[22] = 2;
        //System.arraycopy(intToBytes(2),0,data,40,4);
        data[23] = 0;
        //System.arraycopy(intToBytes(0),0,data,44,4);
        data[24] = 1;
        //System.arraycopy(intToBytes(1),0,data,48,4);
        data[25] = 0;
        //System.arraycopy(intToBytes(0),0,data,52,4);
        String text = "测试文字";
        try {
            byte[] textB = text.getBytes("GBK");
            Log.i("文字", textB.length + "");
            System.arraycopy(textB, 0, data, 26, textB.length);
            data[34] = '\0';
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        TaskCenter.sharedCenter().send(data);
    }

    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[3] = (byte) ((value >> 24) & 0xFF);
        return src;
    }

    //随机生成第五行数据
    PollingUtil pollingUtil;
    Runnable runnable;

    public void tempTimer() {
//每3秒打印一次日志
        pollingUtil = new PollingUtil(new Handler(getMainLooper()));
        runnable = new Runnable() {
            @Override
            public void run() {
                setOSDDistance();
                Log.e("OSD", "----------handler 定时轮询任务----------");
            }
        };
        pollingUtil.startPolling(runnable, 5000, true);
    }

    byte[] dataTem;

    public void showOsdInfo() {

            dataTem = new byte[1024];
            dataTem[6] = 0x11;
            dataTem[7] = (byte) 0xa8;
            dataTem[8] = 0x01;
            dataTem[9] = (byte) 0xa4;
            dataTem[10] = 1;// 0-不显示，1-要显示
            dataTem[11] = 6;// 最多要显示的行数  1-12 ，其它值默认为6
            dataTem[12] = 64;// 每行最多需要显示的字符数  1-64，其它值默认为32
            dataTem[13] = 3;// 字体大小，1-16*16，2-32*32，3-48*48，4-64*64，其它值为自动
            dataTem[14] = 1;// 0-动态显示，1-保存显示
            dataTem[15] = 1;// 1-数据有修改，需要显示刷新， 内部用
            dataTem[16] = 1;// 每个text是分一行还是两行来显示，1一行最多64字节，2两行分成两个32字节//bDispLinePerTxet
            dataTem[17] = 1;// 背景,暂无效
            dataTem[18] = 0;//bDispChl;   // 在哪个通道上显示 1-3 0默认
            dataTem[19] = 0;// 6-每行可以单独指定位置，0-只需指定第一行位置
            //中间保留2个字节
            //文字部分，目前是4行显示，除去文字信息，还能使用64个字节
            //byte[] tasknameB;
            try {
                //写入第一行信息
                String taskname = OsdSharePreference.getInstance(mContext).getString("taskname");
                taskname = "检测任务:" + taskname;
                dataTem[22] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
                dataTem[23] = 0;//bXPos
                dataTem[24] = 1;//bYPos;
                dataTem[25] = 0;//bColor;   // 色彩
                byte[] tasknameB = taskname.getBytes("GBK");
                System.arraycopy(tasknameB, 0, dataTem, 26, tasknameB.length);
                dataTem[26 + tasknameB.length] = '\0';
                //写入第二行信息
                dataTem[90] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
                dataTem[91] = 0;//bXPos
                dataTem[92] = 2;//bYPos;
                dataTem[93] = 0;//bColor;   // 色彩
                String wellname = OsdSharePreference.getInstance(mContext).getString("wellname");
                wellname = "井号信息:" + wellname;
                byte[] wellnameB = wellname.getBytes("GBK");
                System.arraycopy(wellnameB, 0, dataTem, 94, wellnameB.length);
                dataTem[94 + wellnameB.length] = '\0';
                //写入第三行信息
                dataTem[158] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
                dataTem[159] = 0;//bXPos
                dataTem[160] = 3;//bYPos;
                dataTem[161] = 0;//bColor;   // 色彩
                String checkinfo = OsdSharePreference.getInstance(mContext).getString("checkinfo");
                checkinfo = "检测信息:" + checkinfo;
                byte[] checkinfoB = checkinfo.getBytes("GBK");
                System.arraycopy(checkinfoB, 0, dataTem, 162, checkinfoB.length);
                dataTem[162 + checkinfoB.length] = '\0';
                //写入第4行信息
                dataTem[226] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
                dataTem[227] = 0;//bXPos
                dataTem[228] = 4;//bYPos;
                dataTem[229] = 0;//bColor;   // 色彩
                String checkcompany = OsdSharePreference.getInstance(mContext).getString("checkcompany");
                checkcompany = "检测单位:" + checkcompany;
                byte[] checkcompanyB = checkcompany.getBytes("GBK");
                System.arraycopy(checkcompanyB, 0, dataTem, 230, checkcompanyB.length);
                dataTem[230 + checkcompanyB.length] = '\0';
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            /*String text = "测试文字";
            byte[] textB = text.getBytes("GBK");
            Log.i("文字", textB.length + "");
            System.arraycopy(textB, 0, data, 26, textB.length);
            data[34] = '\0';*/
                //写入第5行距离动态信息
               /* dataTem[294] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
                dataTem[295] = 0;//bXPos
                dataTem[296] = 5;//bYPos;
                dataTem[297] = 0;//bColor;   // 色彩
                Random random = new Random();
                int dis = random.nextInt(5) + 100;
                String distance = "距离:" + dis + "米";
                //checkcompany="检测单位:"+checkcompany;
                byte[] distanceB = distance.getBytes("GBK");
                System.arraycopy(distanceB, 0, dataTem, 298, distanceB.length);
                dataTem[298 + distanceB.length] = '\0';
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }*/
         /*else {
            dataTem[14] = 1;
            dataTem[22] = 1;
            dataTem[90] = 1;
            dataTem[158] = 1;
            dataTem[226] = 1;
            //写入第5行距离动态信息
            dataTem[294] = 2;// 0-不显示，1-要显示，2-要显示并且数据有改动
            dataTem[295] = 0;//bXPos
            dataTem[296] = 5;//bYPos;
            dataTem[297] = 0;//bColor;   // 色彩
            Random random = new Random();
            int dis = random.nextInt(5) + 100;
            String distance = "距离:" + dis + "米";
            //checkcompany="检测单位:"+checkcompany;
            //byte[] distanceB = new byte[0];
            try {
                byte[] distanceB = distance.getBytes("GBK");
                System.arraycopy(distanceB, 0, dataTem, 298, distanceB.length);
                dataTem[298 + distanceB.length] = '\0';
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }*/
        TaskCenter.sharedCenter().send(dataTem);
    }

    byte[] dataDis;

    public void setOSDDistance() {
        if (dataDis == null) {
            dataDis = new byte[1024];
            dataDis[6] = 0x11;
            dataDis[7] = (byte) 0x90;
            dataDis[8] = 0x0;
            dataDis[9] = (byte) 0x60;
            dataDis[10] = 1;
            dataDis[12] = 1;
            //dataDis[14] = 1;//字符串1
            Random random = new Random();
            int dis = random.nextInt(5) + 100;
            String distance = "距离:" + dis + "米";
            try {
                byte[] distanceB = distance.getBytes("GBK");
                System.arraycopy(distanceB, 0, dataDis, 14, distanceB.length);
                dataDis[14 + distanceB.length] = '\0';
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            dataDis[82] = 13;
        }
        TaskCenter.sharedCenter().send(dataDis);
    }
}

