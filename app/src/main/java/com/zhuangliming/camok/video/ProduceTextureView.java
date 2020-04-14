/*
 *
 *  *
 *  *  * Copyright (C) 2017 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.zhuangliming.camok.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.Nullable;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLSurfaceTextureProducerView;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.decode.MediaCodecDecoder;


/**
 * Created by Chilling on 2016/11/3.
 */

public class ProduceTextureView extends GLSurfaceTextureProducerView {

    private TextureFilter textureFilter = new BasicTextureFilter();
    private Bitmap bitmap;
    private Surface mSurface;
    private MediaCodecDecoder mediaCodecDecoder;
    public ProduceTextureView(Context context) {
        super(context);
    }

    public ProduceTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProduceTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMediaCodecDecoder(MediaCodecDecoder mediaCodecDecoder) {
        this.mediaCodecDecoder = mediaCodecDecoder;

    }

    @Override
    public void onSurfaceCreated() {
        Log.i("TextureView","onSurfaceCreated");
        //bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.lenna);
        setProducedTextureTarget(GLES20.GL_TEXTURE_2D);
        super.onSurfaceCreated();
    }

    public void setTextureFilter(TextureFilter textureFilter) {
        this.textureFilter = textureFilter;
    }


    @Override
    protected void onGLDraw(ICanvasGL canvas, SurfaceTexture producedSurfaceTexture, RawTexture producedRawTexture, @Nullable SurfaceTexture sharedSurfaceTexture, @Nullable BasicTexture sharedTexture) {
        Log.i("TextureView","onGLDraw");
        canvas.drawBitmap(bitmap, 0, 0);
        //}
        //TestVideoEncoder.drawRect(canvas, drawCnt);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        super.onSurfaceTextureAvailable(surface, width, height);
        Log.i("TextureView","onSurfaceTextureAvailable");
        mSurface = new Surface(getSurfaceTexture());
        // 配置解码器
        Log.i("TextureView",this.mediaCodecDecoder==null?"空":"不为空");
        this.mediaCodecDecoder.configure(mSurface);
        System.out.println("配置解码器");
        // 启动解码器
        this.mediaCodecDecoder.start();
        System.out.println("启动解码器");

    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i("TextureView","onSurfaceTextureSizeChanged");

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i("TextureView","onSurfaceTextureDestroyed");
        //mediaCodecDecoder.release();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.i("TextureView","onSurfaceTextureUpdated");
    }
}
