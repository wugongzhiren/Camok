package com.zhuangliming.cam.view;

import android.content.Context;
import android.os.Environment;

import com.zhuangliming.cam.model.MediaItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by ryze on 2016-5-26.
 */
public class FileUtil {

    //系统保存截图的路径
    public static final String SCREENCAPTURE_PATH = "ScreenCapture" + File.separator + "Screenshots" + File.separator;
//  public static final String SCREENCAPTURE_PATH = "ZAKER" + File.separator + "Screenshots" + File.separator;

    public static final String SCREENSHOT_NAME = "Screenshot";

    public static String getAppPath(Context context) {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {


            return Environment.getExternalStorageDirectory().toString();

        } else {

            return context.getFilesDir().toString();
        }

    }


    public static String getScreenShots(Context context) {

        StringBuffer stringBuffer = new StringBuffer(getAppPath(context));
        stringBuffer.append(File.separator);

        stringBuffer.append(SCREENCAPTURE_PATH);

        File file = new File(stringBuffer.toString());

        if (!file.exists()) {
            file.mkdirs();
        }

        return stringBuffer.toString();

    }

    public static String getScreenShotsName(Context context) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");

        String date = simpleDateFormat.format(new Date());

        StringBuffer stringBuffer = new StringBuffer(getScreenShots(context));
        stringBuffer.append(SCREENSHOT_NAME);
        stringBuffer.append("_");
        stringBuffer.append(date);
        stringBuffer.append(".png");

        return stringBuffer.toString();

    }

    public static List<MediaItem> getPhotoItems(){
        String path= Environment.getExternalStorageDirectory() + File.separator +"TUTK_PHOTO";
        File file=new File(path);
        List<MediaItem> items=new ArrayList<>();
        if(file.isDirectory()){
           File[] photos= file.listFiles();
           for(int i=0;i<photos.length;i++){
               items.add(new MediaItem(photos[i].getAbsolutePath(),photos[i].getName(),0));
           }

        }
        return items;
    }
    public static List<MediaItem> getVideoItems(){
        String dirpath=Environment.getExternalStorageDirectory().getAbsolutePath() + "/TUTK_VIDEOS";
        //SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.CHINA);
        File file=new File(dirpath);
        List<MediaItem> items=new ArrayList<>();
        if(file.isDirectory()){
            File[] videos= file.listFiles();
            for(int i=0;i<videos.length;i++){
                items.add(new MediaItem(videos[i].getAbsolutePath(),videos[i].getName(),1));
            }

        }
        return items;
    }

}
