package com.example.tmdt.ui.exchange;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.example.tmdt.Contract;
import com.example.tmdt.databinding.ViewExHeaderBinding;
import com.example.tmdt.ui.messages.ChatFormat;
import com.google.android.material.snackbar.Snackbar;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Thành phần giao diện dùng chung cho các màn của Thành viên 4 (UC23: hợp đồng). */
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

    /** Nhãn trạng thái bo tròn: xanh lá (ổn), cam (chờ), đỏ (lỗi/hủy), xanh dương (thông tin), xám. */
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

    public static String date(long millis) { return millis <= 0 ? "" : Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE); }
    public static String dateTime(long millis) { return millis <= 0 ? "" : Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE_TIME); }
    public static String money(long value) { return String.format(Locale.forLanguageTag("vi-VN"), "%,dđ", value); }
    public static int dp(Context context, float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

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

    /** Ảnh đại diện đã lưu; thiếu ảnh thì hiển thị chữ cái đầu. */
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
}
