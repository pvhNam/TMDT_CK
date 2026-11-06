package com.example.tmdt.ui.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import com.example.tmdt.AdminStats;
import com.example.tmdt.ui.common.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * Biểu đồ cột (thống kê theo khoảng thời gian từ ngày – đến ngày). Vẽ bằng Canvas, không cần thư viện ngoài.
 * Trục dọc có 4 vạch chia; số trên đầu mỗi cột; nhãn dưới cột tự thưa ra khi có nhiều cột.
 */
public final class BarChartView extends View {
    private final List<AdminStats.Bucket> buckets = new ArrayList<>();
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG), grid = new Paint(Paint.ANTI_ALIAS_FLAG),
            text = new Paint(Paint.ANTI_ALIAS_FLAG), value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public BarChartView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        bar.setColor(Ui.BLUE);
        grid.setColor(Ui.BORDER); grid.setStrokeWidth(dp(1));
        text.setColor(Ui.MUTED); text.setTextSize(dp(10.5f));
        value.setColor(Ui.INK); value.setTextSize(dp(11)); value.setFakeBoldText(true); value.setTextAlign(Paint.Align.CENTER);
        setMinimumHeight((int) dp(230));
    }
    private float dp(float v) { return v * density; }

    public void setData(List<AdminStats.Bucket> data) {
        buckets.clear(); buckets.addAll(data);
        StringBuilder description = new StringBuilder("Biểu đồ cột. ");
        for (AdminStats.Bucket b : data) if (b.count > 0) description.append(b.label).append(": ").append(b.count).append(". ");
        setContentDescription(AdminStats.total(data) == 0 ? "Biểu đồ cột: không có dữ liệu trong khoảng đã chọn." : description.toString());
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, (int) dp(230));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(30), right = getWidth() - dp(6), top = dp(18), bottom = getHeight() - dp(34);
        int max = Math.max(1, AdminStats.max(buckets));
        int step = Math.max(1, (int) Math.ceil(max / 4.0));
        int ceiling = step * 4;
        text.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            float y = bottom - (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, grid);
            canvas.drawText(String.valueOf(step * i), left - dp(5), y + dp(4), text);
        }
        if (buckets.isEmpty()) return;
        float slot = (right - left) / buckets.size();
        float width = Math.min(dp(36), slot * 0.62f);
        int every = Math.max(1, (int) Math.ceil(buckets.size() / Math.max(1f, (right - left) / dp(46))));
        text.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < buckets.size(); i++) {
            AdminStats.Bucket b = buckets.get(i);
            float cx = left + slot * i + slot / 2f;
            float h = (bottom - top) * b.count / (float) ceiling;
            if (b.count > 0) {
                canvas.drawRoundRect(new RectF(cx - width / 2f, bottom - h, cx + width / 2f, bottom), dp(4), dp(4), bar);
                if (slot >= dp(16)) canvas.drawText(String.valueOf(b.count), cx, bottom - h - dp(4), value);
            }
            if (i % every == 0) {
                String label = b.label.length() > 8 ? b.label.substring(0, b.label.indexOf(' ') > 0 ? b.label.indexOf(' ') : 8) : b.label;
                canvas.drawText(label, cx, bottom + dp(16), text);
            }
        }
    }
}
