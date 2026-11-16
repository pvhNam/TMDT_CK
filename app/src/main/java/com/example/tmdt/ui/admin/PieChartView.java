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
 * Biểu đồ tròn dạng vành khuyên: mỗi phần một màu theo {@link #COLORS}, giữa ghi tổng số.
 * Chú giải (màu, tên, số lượng, tỷ lệ %) do màn thống kê dựng bên dưới bằng cùng bảng màu.
 */
public final class PieChartView extends View {
    /** Bảng màu dùng chung cho phần tròn và chú giải. */
    public static final int[] COLORS = {0xFF006BFF, 0xFF009D70, 0xFFEF8700, 0xFFFF364C, 0xFF7B61FF, 0xFF00A6C8, 0xFFAEB9C9};
    private final List<AdminStats.Slice> slices = new ArrayList<>();
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG), total = new Paint(Paint.ANTI_ALIAS_FLAG), caption = new Paint(Paint.ANTI_ALIAS_FLAG),
            empty = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private String unit = "";

    public PieChartView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        arc.setStyle(Paint.Style.STROKE); arc.setStrokeWidth(dp(30));
        empty.setStyle(Paint.Style.STROKE); empty.setStrokeWidth(dp(30)); empty.setColor(Ui.BORDER);
        total.setColor(Ui.INK); total.setTextSize(dp(24)); total.setFakeBoldText(true); total.setTextAlign(Paint.Align.CENTER);
        caption.setColor(Ui.MUTED); caption.setTextSize(dp(12)); caption.setTextAlign(Paint.Align.CENTER);
    }
    private float dp(float v) { return v * density; }
    public static int color(int index) { return COLORS[Math.min(index, COLORS.length - 1)]; }

    /** @param unit ví dụ "đăng ký" hoặc "lớp", ghi dưới tổng số ở giữa */
    public void setData(List<AdminStats.Slice> data, String unit) {
        slices.clear(); slices.addAll(data); this.unit = unit;
        StringBuilder description = new StringBuilder("Biểu đồ tròn. ");
        for (AdminStats.Slice s : data) description.append(s.label).append(" ").append(s.count).append(", ").append(s.percentLabel()).append(". ");
        setContentDescription(data.isEmpty() ? "Biểu đồ tròn: không có dữ liệu." : description.toString());
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, (int) dp(210));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight()) - dp(40);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        RectF box = new RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);
        int sum = 0; for (AdminStats.Slice s : slices) sum += s.count;
        if (sum == 0) { canvas.drawArc(box, 0, 360, false, empty); }
        else {
            float start = -90;
            for (int i = 0; i < slices.size(); i++) {
                float sweep = 360f * slices.get(i).count / sum;
                arc.setColor(color(i));
                canvas.drawArc(box, start, Math.max(0.5f, sweep - (slices.size() > 1 ? 1.2f : 0)), false, arc);
                start += sweep;
            }
        }
        canvas.drawText(String.valueOf(sum), cx, cy + dp(6), total);
        canvas.drawText(unit, cx, cy + dp(24), caption);
    }
}
