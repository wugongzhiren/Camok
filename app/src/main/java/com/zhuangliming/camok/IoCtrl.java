package com.zhuangliming.camok;


import java.io.IOException;

public class IoCtrl {

    public final byte MOTOR_STOP=0;
    public final byte MOTOR_UP=1;
    public final byte MOTOR_DOWN=2;

    public static  void initTCP(){
        TaskCenter.sharedCenter().connect("192.168.1.102",9999);
        TaskCenter.sharedCenter().setDisconnectedCallback(new TaskCenter.OnServerDisconnectedCallbackBlock() {
            @Override
            public void callback(IOException e) {

            }
        });
        TaskCenter.sharedCenter().setConnectedCallback(new TaskCenter.OnServerConnectedCallbackBlock() {
            @Override
            public void callback() {

            }
        });
        TaskCenter.sharedCenter().setReceivedCallback(new TaskCenter.OnReceiveCallbackBlock() {
            @Override
            public void callback(String receicedMessage) {

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
}

