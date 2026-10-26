package com.example.tmdt.ui.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import com.example.tmdt.R;
import com.example.tmdt.databinding.ViewAvatarBinding;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AvatarRenderer {
    private static final android.util.LruCache<String,Bitmap> photos=new android.util.LruCache<String,Bitmap>(2*1024*1024){
        @Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount()+key.length()*2;}
    };
    private static final ExecutorService decode=Executors.newFixedThreadPool(2);
    private static final Handler main=new Handler(Looper.getMainLooper());
    // Accessed on the UI thread. Multiple avatars can share one pending decode.
    private static final Map<String,List<WeakReference<ViewAvatarBinding>>> pending=new HashMap<>();
    private AvatarRenderer(){}
    public static void render(ViewAvatarBinding binding,String encoded,String name){
        name=name==null?"":name.trim();
        binding.avatarInitials.setText(name.isEmpty()?"HV":name.substring(0,name.offsetByCodePoints(0,1)).toUpperCase(Locale.ROOT));
        String source=encoded==null||encoded.length()>90000?"":encoded;
        if(source.equals(binding.avatarPhoto.getTag(R.id.avatar_render_source)))return;
        binding.avatarPhoto.setTag(R.id.avatar_render_source,source);
        binding.avatarPhoto.setClipToOutline(true);
        Bitmap cached=photos.get(source);show(binding,cached);
        if(source.isEmpty()||cached!=null)return;
        List<WeakReference<ViewAvatarBinding>> waiting=pending.get(source);
        if(waiting!=null){waiting.add(new WeakReference<>(binding));return;}
        waiting=new ArrayList<>();waiting.add(new WeakReference<>(binding));pending.put(source,waiting);
        decode.execute(()->{
            Bitmap bitmap=decodePhoto(source);
            if(bitmap!=null)photos.put(source,bitmap);
            main.post(()->{
                List<WeakReference<ViewAvatarBinding>> targets=pending.remove(source);
                if(targets==null)return;
                for(WeakReference<ViewAvatarBinding> target:targets){
                    ViewAvatarBinding current=target.get();
                    // A recycled view may now belong to another user or have no photo.
                    if(current!=null&&source.equals(current.avatarPhoto.getTag(R.id.avatar_render_source)))show(current,bitmap);
                }
            });
        });
    }
    private static Bitmap decodePhoto(String source){
        try{
            byte[] bytes=Base64.decode(source,Base64.DEFAULT);
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(options.outWidth<=0||options.outHeight<=0)return null;
            options.inJustDecodeBounds=false;options.inSampleSize=1;
            while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>256)options.inSampleSize*=2;
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        }catch(IllegalArgumentException ignored){return null;}
    }
    private static void show(ViewAvatarBinding binding,Bitmap bitmap){
        binding.avatarPhoto.setImageBitmap(bitmap);
        binding.avatarPhoto.setVisibility(bitmap==null?View.GONE:View.VISIBLE);
        binding.avatarInitials.setVisibility(bitmap==null?View.VISIBLE:View.GONE);
    }
}
