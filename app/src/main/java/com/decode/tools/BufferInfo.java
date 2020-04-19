package com.decode.tools;

import android.util.Log;

/**
 * videobuffer信息类
 */

public class BufferInfo {
    public int len;// videobuffer的有效长度
    public byte[] buffer;// videobuffer
    public boolean isIframe=false; //关键帧
    public long index;//标记当前帧顺序
    public BufferInfo(int len,byte[] buffer,long index){
        this.len=len;
        this.buffer=buffer;
        this.index=index;
        int type = buffer[4] & 0x1f;
        Log.i("saveFrames","I帧是"+type);
        if (type == 7) {
            this.isIframe = true;
        }
    }
}
