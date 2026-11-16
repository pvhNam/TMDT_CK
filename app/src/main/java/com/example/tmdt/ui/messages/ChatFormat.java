package com.example.tmdt.ui.messages;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.tmdt.ui.common.AvatarView;
import android.view.ViewGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Định dạng thời gian và ảnh đại diện dùng chung cho màn 05 và 06. */
public final class ChatFormat {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] WEEKDAYS = {"Th 2", "Th 3", "Th 4", "Th 5", "Th 6", "Th 7", "CN"};

    private ChatFormat() {}

    private static ZonedDateTime at(long millis) { return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()); }

    /** Giờ gửi một tin: 09:15. */
    public static String clock(long millis) { return millis <= 0 ? "" : at(millis).format(CLOCK); }

    /** Thời gian trên danh sách: 09:30, Hôm qua, Th 5, 12/08. */
    public static String listTime(long millis) {
        if (millis <= 0) return "";
        ZonedDateTime time = at(millis);
        long days = ChronoUnit.DAYS.between(time.toLocalDate(), LocalDate.now());
        if (days <= 0) return time.format(CLOCK);
        if (days == 1) return "Hôm qua";
        if (days < 7) return WEEKDAYS[time.getDayOfWeek().getValue() - 1];
        return time.format(SHORT_DATE);
    }

    /** Nhãn ngày chia nhóm tin nhắn. */
    public static String day(long millis) {
        LocalDate date = at(millis).toLocalDate();
        long days = ChronoUnit.DAYS.between(date, LocalDate.now());
        if (days == 0) return "Hôm nay";
        if (days == 1) return "Hôm qua";
        return date.format(DATE);
    }
    public static LocalDate date(long millis) { return at(millis).toLocalDate(); }

    /** "Hoạt động 5 phút trước" khi người kia không trực tuyến. */
    public static String lastSeen(long millis) {
        if (millis <= 0) return "";
        long minutes = Math.max(1, (System.currentTimeMillis() - millis) / 60000);
        if (minutes < 60) return "Hoạt động " + minutes + " phút trước";
        long hours = minutes / 60;
        if (hours < 24) return "Hoạt động " + hours + " giờ trước";
        long days = hours / 24;
        return days == 1 ? "Hoạt động hôm qua" : "Hoạt động " + days + " ngày trước";
    }

    /** Chữ cái đầu của tên gọi (từ cuối trong tên tiếng Việt). */
    public static String initial(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
        return parts[parts.length - 1].substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public static void avatar(ImageView photo, TextView initials, String name) {
        initials.setText(initial(name));
        photo.setImageDrawable(null);
        photo.setVisibility(View.GONE);
        initials.setVisibility(View.VISIBLE);
    }

    /** Displays the same saved profile photo as the home/profile screens, keyed by Firebase UID. */
    public static void avatar(ImageView photo, TextView initials, String uid, String name) {
        ViewGroup container=(ViewGroup)photo.getParent();
        AvatarView avatar=container.findViewWithTag("chat_profile_avatar");
        if(avatar==null){avatar=new AvatarView(container.getContext());avatar.setTag("chat_profile_avatar");container.addView(avatar,new ViewGroup.LayoutParams(-1,-1));}
        photo.setVisibility(View.GONE);initials.setVisibility(View.GONE);
        avatar.bind(uid,name);
    }
}
