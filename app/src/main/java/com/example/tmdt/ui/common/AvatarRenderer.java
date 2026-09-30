package com.example.tmdt.ui.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import com.example.tmdt.databinding.ViewAvatarBinding;
import java.util.Locale;

public final class AvatarRenderer {
    private AvatarRenderer(){}
    public static void render(ViewAvatarBinding binding,String encoded,String name){
        Bitmap bitmap=null;
        if(!encoded.isEmpty()&&encoded.length()<=90000){
            try {byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);}
            catch(IllegalArgumentException ignored){}
        }
        binding.avatarInitials.setText(name.isEmpty()?"HV":name.substring(0,1).toUpperCase(Locale.ROOT));
        binding.avatarPhoto.setClipToOutline(true);
        binding.avatarPhoto.setImageBitmap(bitmap);
        binding.avatarPhoto.setVisibility(bitmap==null?View.GONE:View.VISIBLE);
        binding.avatarInitials.setVisibility(bitmap==null?View.VISIBLE:View.GONE);
    }
}

