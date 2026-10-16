package com.example.tmdt.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.TutorReviewState;
import com.google.firebase.auth.FirebaseAuth;
import com.example.tmdt.TutorEvidenceAccess;
import com.google.firebase.firestore.Blob;
import java.util.Map;

/** Authenticated server read of a private Firestore image document. */
public final class EvidenceImageView extends FrameLayout {
    private static final java.util.concurrent.ExecutorService DECODE=java.util.concurrent.Executors.newFixedThreadPool(2);
    private final ImageView image;
    private final TextView message;
    private final String path,name;
    private final Map<String,Object> fileMetadata;
    private Bitmap bitmap;
    private int generation;
    private final boolean compact;
    public EvidenceImageView(Context context,Map<String,Object> file){
        this(context,file,false);
    }
    public EvidenceImageView(Context context,Map<String,Object> file,boolean compact){
        super(context);this.compact=compact;Ui ui=new Ui(context);fileMetadata=new java.util.HashMap<>(file);path=TutorReviewState.text(file,"path");name=TutorReviewState.text(file,"name");
        setLayoutParams(new LinearLayout.LayoutParams(compact?ui.dp(52):-1,ui.dp(compact?52:168)));setMinimumHeight(ui.dp(compact?52:168));ui.surface(this,Ui.PALE,10,Ui.BORDER);setClipToOutline(true);
        image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setPadding(ui.dp(8),ui.dp(8),ui.dp(8),ui.dp(8));
        image.setContentDescription("Ảnh minh chứng: "+name);addView(image,new LayoutParams(-1,-1));
        message=ui.text(compact?"…":"Đang tải ảnh minh chứng…",compact?11:13,Ui.MUTED,false);message.setGravity(Gravity.CENTER);addView(message,new LayoutParams(-1,-1));
        setOnClickListener(v->{if(bitmap==null){load();return;}
            ImageView large=new ImageView(context);large.setImageBitmap(bitmap);large.setAdjustViewBounds(true);large.setScaleType(ImageView.ScaleType.FIT_CENTER);
            new AlertDialog.Builder(context).setTitle(name).setView(large).setPositiveButton("Đóng",null).show();
        });
        Motion.press(this);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();load();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        super.onMeasure(widthSpec,MeasureSpec.makeMeasureSpec(Math.round((compact?52:168)*getResources().getDisplayMetrics().density),MeasureSpec.EXACTLY));
    }
    private void load(){
        String uid=FirebaseAuth.getInstance().getUid();if(uid==null||path.isEmpty())return;
        int request=++generation;message.setText(compact?"…":"Đang tải ảnh minh chứng…");message.setVisibility(VISIBLE);
        if(!path.matches("tutor_applications/[^/]+/evidence_images/[a-zA-Z0-9-]+")){message.setText("Ảnh từ bản lưu cũ. Vui lòng tải lại minh chứng.");return;}
        TutorEvidenceAccess.read(java.util.Collections.unmodifiableMap(fileMetadata)).addOnCompleteListener(task->{
            if(!valid(request,uid))return;
            if(!task.isSuccessful()){
                String detail=TutorEvidenceAccess.message(task.getException());
                message.setText(compact?"Thử lại":detail+"\nChạm để thử lại.");
                setContentDescription(detail+". Chạm để thử lại.");return;
            }
            Object stored=task.getResult().get("anh");
            if(!(stored instanceof Blob)){message.setText("Không tìm thấy ảnh minh chứng. Vui lòng tải lại.");return;}
            byte[] data=((Blob)stored).toBytes();
            if(data.length>com.example.tmdt.FirestoreEvidenceImage.MAX_BYTES){message.setText("Ảnh vượt quá kích thước cho phép.");return;}
            DECODE.execute(()->{
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,options);
                options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1600)options.inSampleSize*=2;
                options.inJustDecodeBounds=false;Bitmap decoded=BitmapFactory.decodeByteArray(data,0,data.length,options);
                post(()->{if(!valid(request,uid)){if(decoded!=null)decoded.recycle();return;}bitmap=decoded;
                    if(decoded==null){message.setText("Ảnh không hợp lệ. Vui lòng yêu cầu người gửi thay ảnh.");return;}
                    image.setImageBitmap(decoded);message.setVisibility(GONE);Motion.reveal(image);
                });
            });
        });
    }
    private boolean valid(int request,String uid){return request==generation&&isAttachedToWindow()&&uid.equals(FirebaseAuth.getInstance().getUid());}
    @Override protected void onDetachedFromWindow(){generation++;image.setImageDrawable(null);bitmap=null;super.onDetachedFromWindow();}
}
