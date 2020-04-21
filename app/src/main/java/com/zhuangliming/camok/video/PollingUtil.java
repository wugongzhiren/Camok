package com.zhuangliming.camok.video;

import android.os.Handler;

import java.util.HashMap;
import java.util.Map;

public class PollingUtil {
    private Handler mHanlder;
    private Map<Runnable, Runnable> mTaskMap = new HashMap<Runnable, Runnable>();

    public PollingUtil(Handler handler) {
        mHanlder = handler;
    }

    /**
     * 开启定时任务
     * @param runnable   任务
     * @param interval   时间间隔
     */
    public void startPolling(Runnable runnable, long interval) {
        startPolling(runnable, interval, false);
    }

    /**
     * 开启定时任务
     * @param runnable   任务
     * @param interval   时间间隔
     * @param runImmediately  是否先立即执行一次
     */
    public void startPolling(final Runnable runnable, final long interval,
                             boolean runImmediately) {
        if (runImmediately) {
            runnable.run();
        }
        Runnable task = mTaskMap.get(runnable);
        if (task == null) {
            task = new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    post(runnable, interval);
                }
            };
            mTaskMap.put(runnable, task);
        }
        post(runnable, interval);
    }

    /**
     * 结束某个定时任务
     * @param runnable  任务
     */
    public void endPolling(Runnable runnable) {
        if (mTaskMap.containsKey(runnable)) {
            mHanlder.removeCallbacks(mTaskMap.get(runnable));
        }
    }

    private void post(Runnable runnable, long interval) {
        Runnable task = mTaskMap.get(runnable);
        mHanlder.removeCallbacks(task);
        mHanlder.postDelayed(task, interval);
    }
}
