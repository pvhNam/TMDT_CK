package com.example.tmdt;

import android.graphics.*;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FieldValue;
import java.io.*;
import java.util.*;

/** Compress each photo into its own private Firestore document, below the 1 MiB document limit. */
public final class FirestoreEvidenceImage {
    public static final int MAX_BYTES=300*1024;
    public final Map<String,Object> metadata,document;
    private FirestoreEvidenceImage(Map<String,Object> metadata,Map<String,Object> document){this.metadata=metadata;this.document=document;}
    public static FirestoreEvidenceImage prepare(File source,String uid,String originalName,String kind)throws IOException {
        if(!Arrays.asList("diploma","certificate").contains(kind))throw new IOException("Loại minh chứng không hợp lệ.");
        Bitmap decoded=ImageDecoder.decodeBitmap(ImageDecoder.createSource(source),(decoder,info,input)->{
            float ratio=Math.min(1f,1800f/Math.max(info.getSize().getWidth(),info.getSize().getHeight()));
            decoder.setTargetSize(Math.max(1,Math.round(info.getSize().getWidth()*ratio)),Math.max(1,Math.round(info.getSize().getHeight()*ratio)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
        });
        Bitmap bitmap=Bitmap.createBitmap(decoded.getWidth(),decoded.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);canvas.drawBitmap(decoded,0,0,null);decoded.recycle();
        byte[] bytes=null;
        try {
            while(true){
                for(int quality:new int[]{88,78,68,58}){
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    if(!bitmap.compress(Bitmap.CompressFormat.JPEG,quality,out))throw new IOException("Không nén được ảnh.");
                    if(out.size()<=MAX_BYTES){bytes=out.toByteArray();break;}
                }
                if(bytes!=null)break;
                if(Math.max(bitmap.getWidth(),bitmap.getHeight())<=1000)throw new IOException("Ảnh có quá nhiều chi tiết để lưu. Hãy chụp sát phần minh chứng hoặc chọn ảnh khác.");
                Bitmap smaller=Bitmap.createScaledBitmap(bitmap,Math.max(1,(int)(bitmap.getWidth()*.85)),Math.max(1,(int)(bitmap.getHeight()*.85)),true);
                bitmap.recycle();bitmap=smaller;
            }
        }finally{bitmap.recycle();}
        String name=originalName==null?"minh_chung":originalName.replaceFirst("(?i)\\.(png|jpe?g)$","");
        name=name.substring(0,Math.min(name.length(),145))+".jpg";
        String path="tutor_applications/"+uid+"/evidence_images/"+UUID.randomUUID();
        Map<String,Object> metadata=new HashMap<>();metadata.put("path",path);metadata.put("name",name);metadata.put("type","image/jpeg");metadata.put("size",bytes.length);metadata.put("kind",kind);
        Map<String,Object> document=new HashMap<>(metadata);document.remove("path");document.put("user_id",uid);document.put("anh",Blob.fromBytes(bytes));document.put("created_at",FieldValue.serverTimestamp());
        return new FirestoreEvidenceImage(metadata,document);
    }
}
