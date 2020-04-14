package com.zhuangliming.camok.video;

import java.util.ArrayList;

public class FFmpegCommandCenter {
    /**
     * 文字水印
     */
    public static String[] addTextMark(String text,String videoUrl,String outputUrl){
        ArrayList<String> _commands=new ArrayList<>();
        _commands.add("ffmpeg");
        //输入
        _commands.add("-i");
        _commands.add(videoUrl);
        //水印
        _commands.add("-i");
        _commands.add(text);
        _commands.add("-filter_complex");
        _commands.add("overlay=(main_w-overlay_w)/2:(main_h-overlay_h)/2");
        //覆盖输出
        _commands.add("-y");//直接覆盖输出文件
        //输出文件
        _commands.add(outputUrl);
        String[] commands = new String[_commands.size()];
        for(int i=0;i<_commands.size();i++){
            commands[i]=_commands.get(i);
        }
        return commands;
    }
}
