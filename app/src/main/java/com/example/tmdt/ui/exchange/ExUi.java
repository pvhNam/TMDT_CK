package com.example.tmdt.ui.exchange;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.Contract;
import com.example.tmdt.R;
import com.example.tmdt.Report;
import com.example.tmdt.ReportState;
import com.example.tmdt.databinding.ViewExHeaderBinding;
import com.example.tmdt.ui.messages.ChatFormat;
import com.google.android.material.snackbar.Snackbar;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Thành phần giao diện dùng chung cho các màn của Thành viên 4. */
public final class ExUi {
    public static final int OK = 0, WAIT = 1, BAD = 2, INFO = 3, NEUTRAL = 4;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");

    private ExUi() {}

    public static void header(ViewExHeaderBinding header, String title, @Nullable String subtitle, Runnable back) {
        header.title.setText(title);
        header.subtitle.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
        if (subtitle != null) header.subtitle.setText(subtitle);
        header.back.setOnClickListener(v -> back.run());
    }

    /** Nhãn trạng thái bo tròn: xanh lá (ổn), cam (chờ), đỏ (lỗi/khóa), xanh dương (thông tin), xám. */
    public static void pill(TextView view, String text, int kind) {
        int fg, bg;
        switch (kind) {
            case OK: fg = 0xFF0E9F6E; bg = 0xFFE3F8EE; break;
            case WAIT: fg = 0xFFC77700; bg = 0xFFFFF3D6; break;
            case BAD: fg = 0xFFE5484D; bg = 0xFFFFE8E8; break;
            case INFO: fg = 0xFF0878FF; bg = 0xFFEAF4FF; break;
            default: fg = 0xFF6A7FA6; bg = 0xFFF0F3F8;
        }
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(bg); shape.setCornerRadius(dp(view.getContext(), 8));
        view.setBackground(shape); view.setTextColor(fg); view.setText(text);
        view.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
    }
    public static int kind(Contract c) {
        if (c.agreed()) return OK;
        if (c.pending()) return WAIT;
        return c.closed() ? BAD : NEUTRAL;
    }
    public static int kind(Report r) {
        switch (r.status) {
            case Report.RESOLVED: return OK;
            case Report.DISMISSED: return NEUTRAL;
            case Report.NEED_INFO: return BAD;
            default: return WAIT;
        }
    }

    /** Hàng sao; truyền onPick để người dùng chọn số sao. */
    public static void stars(LinearLayout row, int rating, int sizeDp, @Nullable IntConsumer onPick) {
        Context context = row.getContext();
        row.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(context);
            star.setImageResource(R.drawable.ic_star);
            star.setImageTintList(ColorStateList.valueOf(i <= rating ? 0xFFFFB617 : 0xFFD8DFEA));
            int size = dp(context, sizeDp);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(dp(context, sizeDp >= 28 ? 10 : 2));
            final int value = i;
            if (onPick != null) {
                star.setContentDescription(value + " sao");
                star.setBackgroundResource(android.R.drawable.list_selector_background);
                star.setOnClickListener(v -> onPick.accept(value));
            } else star.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(star, params);
        }
        if (onPick == null) row.setContentDescription(rating + " trên 5 sao");
    }

    public static String date(long millis) { return millis <= 0 ? "" : Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE); }
    public static String dateTime(long millis) { return millis <= 0 ? "" : Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE_TIME); }
    public static String money(long value) { return String.format(Locale.forLanguageTag("vi-VN"), "%,dđ", value); }
    public static String size(long bytes) {
        return bytes >= 1024 * 1024 ? String.format(Locale.ROOT, "%.1f MB", bytes / 1048576f) : Math.max(1, bytes / 1024) + " KB";
    }
    public static int dp(Context context, float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    /** Bộ đếm ký tự "56/500" dưới ô nhập. */
    public static void counter(EditText input, TextView counter, int max) {
        counter.setText(input.length() + "/" + max);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { counter.setText(s.length() + "/" + max); }
        });
    }
    public static void afterChange(EditText input, java.util.function.Consumer<String> action) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { action.accept(s.toString()); }
        });
    }
    public static String text(EditText input) { return input.getText() == null ? "" : input.getText().toString().trim(); }

    public static void snack(View root, String text) {
        if (root != null && text != null && !text.isEmpty()) Snackbar.make(root, text, Snackbar.LENGTH_LONG).show();
    }

    // Ảnh -------------------------------------------------------------------------------------------

    /** Ảnh đại diện: ảnh base64 của hồ sơ nếu có, nếu không dùng ảnh mẫu hoặc chữ cái đầu. */
    public static void avatar(ImageView photo, TextView initials, @Nullable String base64, String name) {
        Bitmap bitmap = base64 == null || base64.isEmpty() ? null : decode(base64);
        if (bitmap == null) { ChatFormat.avatar(photo, initials, name); return; }
        photo.setImageBitmap(bitmap); photo.setVisibility(View.VISIBLE);
        initials.setVisibility(View.GONE);
    }
    @Nullable public static Bitmap decode(String base64) {
        try { byte[] bytes = Base64.decode(base64, Base64.DEFAULT); return BitmapFactory.decodeByteArray(bytes, 0, bytes.length); }
        catch (IllegalArgumentException e) { return null; }
    }
    /** Xem ảnh minh chứng cỡ lớn. */
    public static void showImage(Context context, String title, String base64) {
        Bitmap bitmap = decode(base64);
        if (bitmap == null) { new AlertDialog.Builder(context).setTitle(title).setMessage("Không đọc được ảnh.").setPositiveButton(R.string.close, null).show(); return; }
        ImageView image = new ImageView(context);
        image.setAdjustViewBounds(true); image.setImageBitmap(bitmap);
        int pad = dp(context, 12); image.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(context).setTitle(title).setView(image).setPositiveButton(R.string.close, null).show();
    }
    /**
     * Đọc ảnh người dùng chọn, thu nhỏ tối đa 1280px và nén JPEG cho tới khi vừa giới hạn lưu trữ.
     * Chạy ở luồng nền.
     */
    public static ReportState.Attachment compress(Context context, Uri uri, String name) throws IOException {
        int[] limits = {1280, 1024, 800};
        for (int edge : limits) {
            Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri), (decoder, info, source) -> {
                float ratio = Math.min(1f, (float) edge / Math.max(info.getSize().getWidth(), info.getSize().getHeight()));
                decoder.setTargetSize(Math.max(1, Math.round(info.getSize().getWidth() * ratio)), Math.max(1, Math.round(info.getSize().getHeight() * ratio)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            for (int quality = 80; quality >= 50; quality -= 15) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bytes);
                String data = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP);
                if (data.length() <= ReportState.MAX_DATA) { bitmap.recycle(); return new ReportState.Attachment(name, data, bytes.size()); }
            }
            bitmap.recycle();
        }
        throw new IOException("Ảnh quá lớn");
    }
}
