package com.example.tmdt.ui.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import com.caverock.androidsvg.SVG;
import com.example.tmdt.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Renders the original 24 × 24 Figma SVG assets without rewriting their paths or colors. */
public final class NavigationAssets {
    private NavigationAssets() {}
    public static void apply(BottomNavigationView navigation){
        int[] ids={R.id.nav_home,R.id.nav_schedule,R.id.nav_messages,R.id.nav_account};
        String[] names={"home","schedule","messages","account"};
        for(int i=0;i<ids.length;i++){
            StateListDrawable states=new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_checked},icon(navigation,names[i]+"_active"));
            states.addState(new int[]{},icon(navigation,names[i]+"_idle"));
            navigation.getMenu().findItem(ids[i]).setIcon(states);
        }
    }
    private static Drawable icon(BottomNavigationView navigation,String name){
        try {
            SVG svg=SVG.getFromAsset(navigation.getContext().getAssets(),"figma/navigation/"+name+".svg");
            int size=Math.round(24*navigation.getResources().getDisplayMetrics().density);
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
            bitmap.setDensity(navigation.getResources().getDisplayMetrics().densityDpi);
            new Canvas(bitmap).drawPicture(svg.renderToPicture(),new RectF(0,0,size,size));
            return new BitmapDrawable(navigation.getResources(),bitmap);
        } catch(Exception e){throw new IllegalStateException("Unable to load Figma navigation asset: "+name,e);}
    }
}
