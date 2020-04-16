package com.zhuangliming.camok.model;

/**
 * 定义EventBus消息
 */
public class MessageEvent {
    public static final int SHOW_PHOTO=1;
    public static final int PREVIEW_VIDEO=2;
    public static final int CONNECT_SUCCESS=3;
    public static final int RECORD_COMPLETE=4;
    public static final int SHOW_OSD=5;
    public static final int DEVICE_CHANGE=6;
    private int message;
    private Object obj;
    public MessageEvent(int message,Object o) {
        this.message = message;
        this.obj=o;
    }

    public int getMessage() {
        return message;
    }

    public void setMessage(int message) {
        this.message = message;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}
