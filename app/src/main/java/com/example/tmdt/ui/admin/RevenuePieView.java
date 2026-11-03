package com.example.tmdt.ui.admin;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.example.tmdt.RevenueBreakdown;
import com.example.tmdt.ui.common.Motion;
import com.example.tmdt.ui.common.Ui;

/** Draws a single lightweight ring without allocating objects during animation frames. */
final class RevenuePieView extends View {
    static final int[] COLORS={Ui.BLUE,0xFF00B79A,0xFFFFB547,0xFF8B78E6};
    private final RevenueBreakdown data;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private final float density;
    private final boolean animate;
    private float progress=1;
    private ValueAnimator animator;
    RevenuePieView(Ui ui,RevenueBreakdown data,boolean animate){
        super(ui.context);this.data=data;density=getResources().getDisplayMetrics().density;this.animate=animate;
        setContentDescription(data.received==0?"Biểu đồ doanh thu: chưa có học phí đã nhận":"Biểu đồ tròn doanh thu theo hình thức học; số tiền và tỷ lệ ở chú giải bên dưới");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        if(animate&&data.received>0&&Motion.enabled()){
            animator=ValueAnimator.ofFloat(0,1);animator.setDuration(650);animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});animator.start();
        }
    }
    @Override protected void onDetachedFromWindow(){if(animator!=null){animator.cancel();animator=null;}super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.43f;
        float stroke=r*.25f;bounds.set(cx-r+stroke/2,cy-r+stroke/2,cx+r-stroke/2,cy+r-stroke/2);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setColor(Ui.PALE);canvas.drawOval(bounds,paint);
        float start=-90;
        if(data.received>0)for(int i=0;i<RevenueBreakdown.LABELS.length;i++){
            float sweep=(float)(data.percent(i)*3.6);paint.setColor(COLORS[i]);
            canvas.drawArc(bounds,start,sweep*progress,false,paint);start+=sweep*progress;
        }
        paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setColor(Ui.INK);paint.setTextSize(25*density);
        canvas.drawText(data.received==0?"0":"100%",cx,cy+2*density,paint);
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setColor(Ui.MUTED);paint.setTextSize(12*density);
        canvas.drawText("Học phí đã nhận",cx,cy+23*density,paint);
    }
}
