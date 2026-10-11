package com.example.tmdt.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/** Static, decorative blur. Cache a small software bitmap for Android 10+ and smooth scrolling. */
public final class SoftBlurBackground extends View {
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect bounds=new Rect();
    private Bitmap backdrop;

    public SoftBlurBackground(Context context,AttributeSet attrs) {
        super(context,attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);setFocusable(false);
    }

    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight) {
        super.onSizeChanged(width,height,oldWidth,oldHeight);
        bounds.set(0,0,width,height);
        if(width<=0||height<=0){backdrop=null;return;}
        // Blur only the decorative colors, never text, input fields or buttons.
        float scale=256f/Math.max(width,height);
        int w=Math.max(1,Math.round(width*scale)),h=Math.max(1,Math.round(height*scale));
        Bitmap next=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(next);
        canvas.drawColor(0xFFFFFFFF);
        Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setMaskFilter(new BlurMaskFilter(Math.max(12f,w*.20f),BlurMaskFilter.Blur.NORMAL));
        glow.setColor(0xFFDCEEFF);
        canvas.drawCircle(w*.62f,h*.13f,w*.36f,glow);
        glow.setColor(0xFFF0F7FF);
        canvas.drawCircle(w*1.08f,h*.93f,w*.52f,glow);
        backdrop=next;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(backdrop!=null)canvas.drawBitmap(backdrop,null,bounds,imagePaint);
    }
}
