package com.tutk.IOTC;

public class OSDType {
    byte[] heard;//10位
    int bFlag;      // 0-不显示，1-要显示
    int	bMaxLine;   // 最多要显示的行数  1-12 ，其它值默认为6
    int	bMaxNum;    // 每行最多需要显示的字符数  1-64，其它值默认为32
    int	bFont ;     // 字体大小，1-16*16，2-32*32，3-48*48，4-64*64，其它值为自动
    int bSave;	    // 0-动态显示，1-保存显示
    int bChange;    // 1-数据有修改，需要显示刷新， 内部用
    byte[] bBlank;//6位
    TextType text;
}
