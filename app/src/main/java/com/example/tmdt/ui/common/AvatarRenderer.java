package com.example.tmdt.ui.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import com.example.tmdt.databinding.ViewAvatarBinding;
import java.util.Locale;

public final class AvatarRenderer {
    private static final android.util.LruCache<String,Bitmap> photos=new android.util.LruCache<String,Bitmap>(2*1024*1024){
        @Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount()+key.length()*2;}
    };
    private AvatarRenderer(){}
    public static void render(ViewAvatarBinding binding,String encoded,String name){
        Bitmap bitmap=encoded==null?null:photos.get(encoded);
        if(bitmap==null&&encoded!=null&&!encoded.isEmpty()&&encoded.length()<=90000){
            try {
                byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(options.outWidth>0 && options.outHeight>0){
                    options.inJustDecodeBounds=false;options.inSampleSize=1;
                    while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>256)options.inSampleSize*=2;
                    bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                    if(bitmap!=null)photos.put(encoded,bitmap);
                }
            }
            catch(IllegalArgumentException ignored){}
        }
        name=name==null?"":name.trim();
        binding.avatarInitials.setText(name.isEmpty()?"HV":name.substring(0,name.offsetByCodePoints(0,1)).toUpperCase(Locale.ROOT));
        binding.avatarPhoto.setClipToOutline(true);
        binding.avatarPhoto.setImageBitmap(bitmap);
        binding.avatarPhoto.setVisibility(bitmap==null?View.GONE:View.VISIBLE);
        binding.avatarInitials.setVisibility(bitmap==null?View.VISIBLE:View.GONE);
    }
}
