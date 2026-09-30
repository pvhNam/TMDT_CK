package com.example.tmdt.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Small, density-independent line icons in the visual style of the reference. */
public final class LineIcon extends View {
    private final String kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF bounds = new RectF();
    private final int color;

    public LineIcon(Context context, String kind, int color) {
        super(context);
        this.kind = kind;
        this.color = color;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void lines(Canvas c, float... xy) {
        path.reset();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        c.drawPath(path, paint);
    }

    private RectF rect(float left,float top,float right,float bottom) {
        bounds.set(left,top,right,bottom);return bounds;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.save();
        float size = Math.min(getWidth(), getHeight());
        c.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        c.scale(size / 24f, size / 24f);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        switch (kind) {
            case "flask":
                lines(c,8,2,16,2);lines(c,9,2,9,9,3,20,4,22,20,22,21,20,15,9,15,2);
                lines(c,7,15,17,15);c.drawPoint(10,18,paint);c.drawPoint(15,20,paint);break;
            case "atom":
                c.drawOval(rect(2,8,22,16),paint);c.save();c.rotate(60,12,12);c.drawOval(rect(2,8,22,16),paint);
                c.rotate(60,12,12);c.drawOval(rect(2,8,22,16),paint);c.restore();c.drawCircle(12,12,1,paint);break;
            case "back": lines(c, 19,12, 5,12, 12,5); lines(c,5,12,12,19); break;
            case "next": lines(c,9,5,16,12,9,19); break;
            case "down": lines(c,6,9,12,15,18,9); break;
            case "search": c.drawCircle(10,10,7,paint); lines(c,15,15,21,21); break;
            case "home": lines(c,2,11,12,2,22,11); lines(c,5,9,5,22,10,22,10,15,14,15,14,22,19,22,19,9); break;
            case "calendar":
                c.drawRoundRect(rect(3,5,21,22),2,2,paint); lines(c,3,10,21,10);
                lines(c,8,2,8,7); lines(c,16,2,16,7); lines(c,7,14,10,14); lines(c,14,14,17,14); lines(c,7,18,10,18); break;
            case "chat":
                c.drawRoundRect(rect(2,3,22,19),3,3,paint); lines(c,6,19,6,23,11,19);
                c.drawPoint(7,11,paint); c.drawPoint(12,11,paint); c.drawPoint(17,11,paint); break;
            case "person": c.drawCircle(12,6,4,paint); c.drawArc(rect(3,12,21,30),180,180,false,paint); lines(c,3,21,21,21); break;
            case "bell":
                lines(c,3,18,5,15,5,10); c.drawArc(rect(5,3,19,17),180,180,false,paint);
                lines(c,19,10,19,15,21,18,3,18); c.drawArc(rect(9,18,15,23),0,180,false,paint);
                paint.setColor(0xFFFF6767); paint.setStyle(Paint.Style.FILL); c.drawCircle(20,3,3.1f,paint); break;
            case "laptop": c.drawRoundRect(rect(4,3,20,17),1,1,paint); lines(c,1,20,23,20); break;
            case "video": c.drawRoundRect(rect(2,5,16,20),2,2,paint); lines(c,16,10,22,6,22,19,16,15); break;
            case "clock": c.drawCircle(12,12,9,paint); lines(c,12,6,12,12,16,14); break;
            case "check": c.drawCircle(12,12,10,paint); lines(c,7,12,10,15,17,8); break;
            case "info": c.drawCircle(12,12,10,paint); lines(c,12,11,12,17); c.drawPoint(12,7,paint); break;
            case "cap": lines(c,1,8,12,3,23,8,12,13,1,8); lines(c,6,11,6,17,12,20,18,17,18,11); lines(c,22,9,22,16); break;
            case "people": c.drawCircle(12,6,3,paint); c.drawCircle(4,9,2,paint); c.drawCircle(20,9,2,paint);
                c.drawArc(rect(6,11,18,26),180,180,false,paint); c.drawArc(rect(0,13,7,23),180,140,false,paint); c.drawArc(rect(17,13,24,23),220,140,false,paint); break;
            case "money": c.drawOval(rect(5,2,19,8),paint); lines(c,5,5,5,19); lines(c,19,5,19,19);
                c.drawArc(rect(5,15,19,22),0,180,false,paint); c.drawArc(rect(5,9,19,16),0,180,false,paint); break;
            case "send": lines(c,2,10,22,2,15,22,10,14,2,10); lines(c,10,14,22,2); break;
            case "heart": case "heart_filled":
                path.reset(); path.moveTo(12,21); path.cubicTo(8,17,1,12,2,7); path.cubicTo(3,1,9,1,12,6);
                path.cubicTo(15,1,21,1,22,7); path.cubicTo(23,12,16,17,12,21);
                if (kind.equals("heart_filled")) paint.setStyle(Paint.Style.FILL);
                c.drawPath(path,paint); break;
            case "star":
                path.reset(); for(int i=0;i<10;i++){ double a=-Math.PI/2+i*Math.PI/5; float r=i%2==0?11:5;
                    float x=12+(float)Math.cos(a)*r,y=12+(float)Math.sin(a)*r; if(i==0)path.moveTo(x,y);else path.lineTo(x,y); }
                path.close(); paint.setStyle(Paint.Style.FILL); c.drawPath(path,paint); break;
            default:
                paint.setStyle(Paint.Style.FILL); paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
                paint.setTextSize(22); paint.setTextAlign(Paint.Align.CENTER); c.drawText(kind,12,20,paint);
        }
        c.restore();
    }
}
