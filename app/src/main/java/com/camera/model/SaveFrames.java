package com.camera.model;

import java.util.ArrayList;

public class SaveFrames {
    public static ArrayList<Frames> frames = new ArrayList<>();
    public boolean canStart = false;
    //当全局变量等于StartReceive == true 时执行
    //将视频帧存入ArrayList中
    public void saveFrames(byte[] videoBuffer, byte[] frameInfo, int ret) {
        Frames frame = new Frames();
        int type = videoBuffer[4] & 0x1f;
        if (type == 7) {
            canStart = true;
        }
        if (canStart) {
            boolean IFrame = false;
            if (frameInfo[2] == 1) {
                IFrame = true;
            }
            frame.setFrame(videoBuffer);
            frame.setIFrame(IFrame);
            frame.setSize(ret);
            frames.add(frame);
        }
    }

    //当全局变量StartReceive == false 时执行
    //当ArrayList非空，执行Muxer后清空ArrayList
    public void stopReceive() {
        canStart = false;
        if (frames.size() != 0) {
            Muxer muxer = new Muxer();
            muxer.muxer(frames);
            frames.clear();
        }
    }
}
