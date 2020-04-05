package com.hankvision.ipcsdk;

import android.view.Surface;

public class Dllipcsdk {
    static {
        System.loadLibrary("native-lib");
    }
    /**
     * 标准码流数据回调函数的指针类型
     * @param [IN]   lRawHandle   当前的原始数据的句柄
     * @param [IN]   nErrorType;	//错误码，取以下值:
     *               0--音视频正常
     *               -1000--连接设备失败，网络不通
     *               -999--登录设备失败，用户名或密码错误
     *               -998--请求视频失败，一般为设备原因
     * @param [IN]   nErrorCode;		//详细错误码，一般用不上
     * @param [IN]   m_AVType;		//音视频类型，1--视频，2--音频
     * @param [IN]   m_EncoderType;  //编码类型，  1--H264，2--MPEG4，3--G711_U（音频），4--H265
     * @param [IN]   m_FrameType;    //帧类型，    1--I帧， 2--P帧，		如果是音频帧，则为0
     * @param [IN]   m_FrameRate;	//帧率，	如果是音频帧，则为0
     * @param [IN]   m_VideoWidth;   //视频宽度，如果是音频帧，则为0
     * @param [IN]   m_VideoHeight;  //视频高度，如果是音频帧，则为0
     * @param [IN]   m_Reserved[3];	//保留
     * @param [IN]   m_Channels;		//通道类型，  1--单声道，2--双声道，如果是视频帧，则为0
     * @param [IN]   m_Samples;      //采样率，  如果是视频帧，则为0
     * @param [IN]   m_BitCount;     //采样位数，如果是视频帧，则为0
     * @param [IN]   m_TimeStamp;    //时间戳
     * @param [IN]   pRawBuffer   存放数据的缓冲区指针
     * @param [IN]   lRawBufSize  存放数据的缓冲区大小
     * @return 无
     * @note
     */
    public interface CBRawData {

        public void RawData(int lRawHandle, int nErrorType, int nErrorCode, int m_AVType, int m_EncoderType, int m_FrameType, int m_FrameRate,
                            int m_VideoWidth, int m_VideoHeight, int m_Channels, int m_Samples, int m_BitCount, int m_TimeStamp,
                            byte pRawBuffer[], int lRawBufSize);
    }

    /**
     * 开始获取标准数据
     * @param [IN]   cIp      登陆的IP
     * @param [IN]   nChannel-0
     * @param [IN]   nStreamMode-0:主码流-1:子码流
     * @param [IN]   fRawData      标准码流数据回调函数
     * @return 返回如下结果：
     * - 失败：-1
     * - 其他值：作为IPCNET_StopRawData等函数的句柄参数
     * - 获取错误码调用IPCNET_GetLastError
     * @note 与实时流获取是相互独立的，即可在不开启实时预览功能时直接获取标准数据
     */
    public static native long IPCNET_StartRawData(String cIp, int nPort, int nChannel, String cUserName, String cPassword, int nStreamMode, CBRawData fRawData);

    /**
     * 停止获取标准数据
     * @param [IN]   lRawHandle    IPCNET_StartRawData的返回值
     * @return 返回如下结果：
     * - 成功：true
     * - 失败：false
     * - 获取错误码调用IPCNET_GetLastError
     * @note 无
     */
    public static native boolean IPCNET_StopRawData(long lRawHandle);

    /*
    * 获取OSD显示状态、位置、大小、文字
    * nIndex:第几行的数据，取值范围：0~4
    * */
    public static native JOSDInfo IPCNET_GetOsdInfo(String strIp, int nHttpPort, int nIndex, String strUsername, String strPassword);

    /*
     * 设置OSD显示状态、位置、大小、文字
     * nIndex:第几行的数据，取值范围：0~4
     * */
    public static native boolean IPCNET_SetOsdInfo(String strIp, int nHttpPort, int nIndex, JOSDInfo josdInfo, String strUsername, String strPassword);

    // 获取倍数显示状态、位置、大小
    public static native JMultipleOsdInfo IPCNET_GetMultipleOsdInfo(String strIp, int nHttpPort, String strUsername, String strPassword);

    // 设置倍数显示状态、位置、大小
    public static native boolean IPCNET_SetMultipleOsdInfo(String strIp, int nHttpPort, JMultipleOsdInfo jMultipleOsdInfo, String strUsername, String strPassword);

    // 获取时间OSD显示状态、位置、大小
    public static native JTimeOsdInfo IPCNET_GetTimeOsdInfo(String strIp, int nHttpPort, String strUsername, String strPassword);

    // 设置时间OSD显示状态、位置、大小
    public static native boolean IPCNET_SetTimeOsdInfo(String strIp, int nHttpPort, JTimeOsdInfo jTimeOsdInfo, String strUsername, String strPassword);

    // 设置摄像机时间
    public static native boolean IPCNET_SetCameraTime(String strIp, int nHttpPort, int nCameraTime, String strUsername, String strPassword);

    /*
    nICRSwitch取值：
    0, "外部控制"
    1, "固定黑白"
    2, "固定彩色"
    3, "程序控制"
    */
    public static native boolean IPCNET_SetICRSwitchInfo(String strIp, int nHttpPort, int nICRSwitch, String strUsername, String strPassword);

    public static native int IPCNET_GetICRSwitchInfo(String strIp, int nHttpPort, String strUsername, String strPassword);

    // 抓拍
    public static native int IPCNET_CapturePicture(String strIp, int nHttpPort, String strUsername, String strPassword, byte pRawBuffer[], int nPicBufSize);

    /*
    * MP4格式录像，nVideoPort：90，nStreamType：0-主码流 1-副码流
    * 返回句柄
    * */
    public static native long IPCNET_StartRecord(String strIp, int nVideoPort, String strFileName, int nStreamType);

    /*
    * IPCNET_StartRecord的返回值
    * */
    public static native void IPCNET_StopRecord(long lHandle);

    public enum E_PTZ_COMMAND
    {
        //基本命令
        ZOOM_TELE,      /**< 焦距变大(倍率变大,视野缩小,目标放大),p1速度 */
        ZOOM_WIDE,      /**< 焦距变小(倍率变小,视野放大,目标缩小),p1速度 */
        FOCUS_NEAR,     /**< 焦点前调(目标靠近),p1速度 */
        FOCUS_FAR,      /**< 焦点后调(目标远离),p1速度 */
        IRIS_OPEN,      /**< 光圈扩大,p1速度 */
        IRIS_CLOSE,     /**< 光圈缩小,p1速度 */
        UP,             /**< 上转,p1水平速度,p2垂直速度 */
        DOWN,           /**< 下转,p1水平速度,p2垂直速度 */
        LEFT,           /**< 左转,p1水平速度,p2垂直速度 */
        RIGHT,          /**< 右转,p1水平速度,p2垂直速度 */
        UP_LEFT,      	/**< 左上,p1水平速度,p2垂直速度 */
        UP_RIGHT,    	/**< 右上,p1水平速度,p2垂直速度 */
        DOWN_LEFT,      /**< 左下,p1水平速度,p2垂直速度 */
        DOWN_RIGHT,     /**< 右下,p1水平速度,p2垂直速度 */

        //预置位操作
        SET_PRESET,     /**< 设置预置点,p1预置点的序号(1-255) */
        GOTO_PRESET,    /**< 转到预置点,p1预置点的序号  */

        //花样扫描
        START_CRUISE,   /**< 开始花样扫描,p1花样扫描的序号(1-4) */
        STOP_CRUISE,    /**< 停止花样扫描,p1花样扫描的序号 */
        RUN_CRUISE,     /**< 运行花样扫描,p1花样扫描的序号 */

        //自动水平运行
        START_AUTO_PAN, /**< 开始自动水平运行,p1自动水平运行的序号(1-4) */
        STOP_AUTO_PAN,  /**< 停止自动水平运行,p1自动水平运行的序号 */
        RUN_AUTO_PAN,   /**< 运行自动水平运行,p1自动水平运行的序号 */

        AUTO_SCAN,      /**< 自动扫描 */
        FLIP,           /**< 翻转 */
        STOP,           /**< 停止 */
        ENTER_MENU,     /**< 进入菜单 */

        //辅助开关/继电器
        AUX_PWRON,      /**< 打开辅助设备开关,p1辅助开关号(1-雨刷,2-灯光,3-加热器) */
        AUX_PWROFF,     /**< 关闭辅助设备开关,p1辅助开关号(1-雨刷,2-灯光,3-加热器) */
    }

    /**
     * 云台控制接口，不用启动预览时也可以使用
     * @param [IN]   lLoginID    登陆的ID，IPCNET_Login的返回值
     * @param [IN]   nPTZCommand 云台控制命令
     * @param [IN]   iParam1     参数1，具体内容跟控制命令有关，详见E_PTZ_COMMAND
     * @param [IN]   iParam2     参数2，同上
     * @param [IN]   bStop       是否停止，对云台八方向操作及镜头操作命令有效，进行其他操作时，本参数应填充false
     * @return 返回如下结果：
     * - 成功：true
     * - 失败：false
     * - 获取错误码调用IPCNET_GetLastError
     * @note 当iParam1表示速度时，范围是1~8
     */
    public static native boolean IPCNET_PTZControl(String strIp, int nVideoPort, int nPTZCommand, int iParam1, int iParam2, boolean bStop);

    /*
     * 实时预览，nVideoPort：90，nStreamType：0-主码流 1-副码流
     * 返回句柄
     * */
    public static native long IPCNET_StartRawPlay(String cIp, int nPort, int nChannel, String cUserName, String cPassword, int nStreamMode, Surface surface);

    public static native boolean IPCNET_StopRawPlay(long lRawPlayHandle);
}
