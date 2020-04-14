package com.zhuangliming.camok.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLMultiTexConsumerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.zhuangliming.camok.R;

import java.util.List;

public class PreviewConsumerTextureView extends GLMultiTexConsumerView {

    private TextureFilter textureFilter = new BasicTextureFilter();
    private Bitmap robot;

    public PreviewConsumerTextureView(Context context) {
        super(context);
    }

    public PreviewConsumerTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreviewConsumerTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onGLDraw(ICanvasGL canvas, List<GLTexture> consumedTextures) {
        if (!consumedTextures.isEmpty()) {
            GLTexture consumedTexture = consumedTextures.get(0);
            SurfaceTexture sharedSurfaceTexture = consumedTexture.getSurfaceTexture();
            RawTexture sharedTexture = consumedTexture.getRawTexture();
            canvas.drawSurfaceTexture(sharedTexture, sharedSurfaceTexture, 0, 0, sharedTexture.getWidth(), sharedTexture.getHeight(), textureFilter);
            canvas.drawBitmap(robot, 0, 0 , 60, 60);
        }
    }

    public void setTextureFilter(TextureFilter textureFilter) {
        this.textureFilter = textureFilter;
    }

    @Override
    protected void init() {
        super.init();
        robot = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);
    }

    public void clearConsumedTextures() {
        consumedTextures.clear();
    }
}
