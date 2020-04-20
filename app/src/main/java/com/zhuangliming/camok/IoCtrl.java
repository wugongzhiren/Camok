package com.zhuangliming.camok;


import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class IoCtrl {

    public final byte MOTOR_STOP=0;
    public final byte MOTOR_UP=1;
    public final byte MOTOR_DOWN=2;

    public static  void initTCP(String ip,int port){
        TaskCenter.sharedCenter().connect(ip,port);
        TaskCenter.sharedCenter().setDisconnectedCallback(new TaskCenter.OnServerDisconnectedCallbackBlock() {
            @Override
            public void callback(IOException e) {

            }
        });
        TaskCenter.sharedCenter().setConnectedCallback(new TaskCenter.OnServerConnectedCallbackBlock() {
            @Override
            public void callback() {
                Log.i("TCP_IO","连接上了TCP");
            }
        });
        TaskCenter.sharedCenter().setReceivedCallback(new TaskCenter.OnReceiveCallbackBlock() {
            @Override
            public void callback(String receicedMessage) {
                Log.i("TCP_IO","接收到了信息："+receicedMessage);
            }
        });
    }

    public  void  writeMotor(int value){
        byte[] buffer = new byte[4];
        buffer[0] = 0x0a;
        buffer[1] = 0x02;
        buffer[2] = (byte)value;
        buffer[3] = (byte)0xe0;
        TaskCenter.sharedCenter().send(buffer);
    }

    public static void  writeOsd(){
        byte[] data=new byte[1024];
        data[6]=0x11;
        data[7]= (byte) 0xa8;
        data[8]=0x01;
        data[9]= (byte) 0xa4;
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
        data[10]=1;
        //System.arraycopy(intToBytes(1),0,data,10,4);
        data[11]=6;
        //System.arraycopy(intToBytes(6),0,data,14,4);
        data[12]=32;
        //System.arraycopy(intToBytes(32),0,data,18,4);
        data[13]=2;
        //System.arraycopy(intToBytes(2),0,data,22,4);
        data[14]=1;
        //System.arraycopy(intToBytes(1),0,data,26,4);
        data[15]=1;
        //System.arraycopy(intToBytes(1),0,data,30,4);
        //中间略过6个字节
        //文字部分
        data[22]=2;
        //System.arraycopy(intToBytes(2),0,data,40,4);
        data[23]=0;
        //System.arraycopy(intToBytes(0),0,data,44,4);
        data[24]=1;
        //System.arraycopy(intToBytes(1),0,data,48,4);
        data[25]=0;
        //System.arraycopy(intToBytes(0),0,data,52,4);
        String text="测试文字";
        try {
            byte[] textB=text.getBytes("GBK");
            Log.i("文字",textB.length+"");
            System.arraycopy(textB,0,data,26,textB.length);
            data[34]='\0';
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        TaskCenter.sharedCenter().send(data);
    }
    public static byte[] intToBytes(int value)
    {
        byte[] src = new byte[4];
        src[0] =  (byte) (value & 0xFF);
        src[1] =  (byte) ((value>>8) & 0xFF);
        src[2] =  (byte) ((value>>16) & 0xFF);
        src[3] =  (byte) ((value>>24) & 0xFF);
        return src;
    }

}

