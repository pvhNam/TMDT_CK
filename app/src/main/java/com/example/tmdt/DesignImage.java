package com.example.tmdt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

/** Displays artwork directly from the user-provided design, without a network dependency. */
final class DesignImage extends View {
    private static Bitmap artwork;
    private final Rect source;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path clip = new Path();
    private final RectF destination = new RectF();
    private final boolean banner;

    DesignImage(Context context, int artworkId) {
        super(context);
        if (artwork == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            artwork = BitmapFactory.decodeResource(getResources(), R.drawable.design_reference, options);
        }
        banner = artworkId >= 2;
        Rect[] regions = {
                new Rect(426, 249, 537, 362), new Rect(1185, 501, 1255, 575),
                new Rect(46, 399, 361, 552), new Rect(1174, 683, 1491, 843)
        };
        source = regions[artworkId];
        setContentDescription(new String[]{"Ảnh gia sư Nguyễn Minh Anh", "Ảnh gia sư Trần Hoàng Nam",
                "Tìm gia sư phù hợp. Linh hoạt thời gian học. Tri thức mở ra tương lai.",
                "Sẵn sàng cho buổi học tiếp theo. Xem lại mục tiêu trước khi bắt đầu."}[artworkId]);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (banner) {
            int width = MeasureSpec.getSize(widthSpec);
            setMeasuredDimension(width, Math.round(width * source.height() / (float) source.width()));
        } else super.onMeasure(widthSpec, heightSpec);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        destination.set(0, 0, getWidth(), getHeight());
        float radius = 11 * getResources().getDisplayMetrics().density;
        clip.reset();
        clip.addRoundRect(destination, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawBitmap(artwork, source, destination, paint);
        canvas.restore();
    }
}
