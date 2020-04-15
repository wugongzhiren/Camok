package com.zhuangliming.camok.video;

import android.util.Log;

import java.util.ArrayList;

public class FFmpegCommandCenter {

    /**
     * 使用ffmpeg命令行给视频添加水印
     * @param srcFile 源文件
     * @param waterMark 水印文件路径
     * @param targetFile 目标文件
     * @return 添加水印后的文件
     */
    public static  String[] addWaterMark(String srcFile, String waterMark, String targetFile){
        String waterMarkCmd = "ffmpeg -i %s -i %s -filter_complex overlay=0:0 %s";
        waterMarkCmd = String.format(waterMarkCmd, srcFile, waterMark, targetFile);
        return waterMarkCmd.split(" ");//以空格分割为字符串数组
    }
    /**
     * 文字水印
     */
    public static String[] addTextMark(String text,String videoUrl,String outputUrl){
        Log.i("FFmpegKit","FFmpegKit addTextMark");
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
