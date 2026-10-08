package com.example.tmdt.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.caverock.androidsvg.SVG;

/** Original Figma SVGs rendered at their own root dimensions in dp. */
public final class TutorDesignAssets {
    private TutorDesignAssets(){}
    public static Drawable icon(Context context,String name){
        try{
            SVG svg=SVG.getFromAsset(context.getAssets(),"figma/tutor_registration/"+name+".svg");
            float density=context.getResources().getDisplayMetrics().density;
            int width=Math.round(svg.getDocumentWidth()*density),height=Math.round(svg.getDocumentHeight()*density);
            Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            bitmap.setDensity(context.getResources().getDisplayMetrics().densityDpi);
            new Canvas(bitmap).drawPicture(svg.renderToPicture(),new RectF(0,0,width,height));
            return new BitmapDrawable(context.getResources(),bitmap);
        }catch(Exception e){throw new IllegalStateException("Cannot load tutor design asset: "+name,e);}
    }
}
