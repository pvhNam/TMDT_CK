package com.example.tmdt.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.tmdt.R;
import com.example.tmdt.Lesson;

/** Shared spacing, typography and components; colours follow the variables of the Figma file "Mobile". */
public final class Ui {
    public static final int INK = 0xFF06164D, MUTED = 0xFF46638C, BLUE = 0xFF006BFF,
            PALE = 0xFFEAF5FF, BORDER = 0xFFD4E5F8, WHITE = 0xFFFFFFFF,
            GREEN = 0xFF009D70, GOLD = 0xFFFFB617, GREEN_BG = 0xFFDDF8EC,
            ORANGE = 0xFFEF8700, ORANGE_BG = 0xFFFFF2D2, RED = 0xFFFF364C, GRAY = 0xFFAEB9C9;
    public static final int PRIMARY = 0, OUTLINE = 1, DANGER = 2, DISABLED = 3;
    public final Context context;

    public Ui(Context context) { this.context = context; }
    public int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    public LinearLayout column() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    public LinearLayout row() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    public void pad(View view, int horizontal, int vertical) { view.setPadding(dp(horizontal),dp(vertical),dp(horizontal),dp(vertical)); }
    public LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }
    public void add(LinearLayout parent, View child) { parent.addView(child, lp(-1,-2)); }
    public void space(LinearLayout parent, int height) { parent.addView(new View(context),lp(1,height)); }
    public void gap(LinearLayout parent, int width) { parent.addView(new View(context),lp(width,1)); }
    public void weight(LinearLayout row, View child) { row.addView(child,new LinearLayout.LayoutParams(0,-2,1)); }
    public void line(LinearLayout parent) {
        View view = new View(context); view.setBackgroundColor(BORDER); parent.addView(view,lp(-1,1));
    }

    public TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setFontFeatureSettings("kern"); view.setIncludeFontPadding(false);
        view.setLayoutParams(lp(-2,-2));
        // The design uses Roboto Condensed, which Android ships as "sans-serif-condensed".
        view.setTypeface(Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setLineSpacing(dp(3),1);
        return view;
    }

    public GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color);
        drawable.setCornerRadius(dp(radius)); if (stroke != 0) drawable.setStroke(dp(1),stroke);
        return drawable;
    }
    public void surface(View view, int color, int radius, int stroke) { view.setBackground(background(color,radius,stroke)); }
    public void clickable(View view, Runnable action) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x220866F5),view.getBackground(),background(WHITE,12,0)));
        view.setOnClickListener(v -> action.run()); view.setFocusable(true);
    }

    public AlertDialog.Builder dialog() {
        return new AlertDialog.Builder(context, R.style.ThemeOverlay_TMDT_Classroom_Dialog) {
            @Override public AlertDialog show() {
                AlertDialog dialog = super.show();
                TextView message = dialog.findViewById(android.R.id.message);
                if (message != null) message.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
                return dialog;
            }
        };
    }

    public LinearLayout card() { LinearLayout card = column(); pad(card,12,12); surface(card,WHITE,12,BORDER); return card; }
    public ScrollView scroll(LinearLayout parent) {
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false); parent.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); return scroll;
    }
    public View iconButton(String icon, String description, Runnable action) {
        LinearLayout button = row(); button.setGravity(Gravity.CENTER);
        button.addView(new LineIcon(context,icon,INK),lp(23,23)); button.setContentDescription(description);
        clickable(button,action); button.setLayoutParams(lp(48,48)); return button;
    }

    public View iconButton(int icon, int color, String description, Runnable action) {
        LinearLayout button = row(); button.setGravity(Gravity.CENTER);
        button.addView(icon(icon,22,color)); button.setContentDescription(description);
        clickable(button,action); button.setLayoutParams(lp(48,48)); return button;
    }

    public LinearLayout header(LinearLayout root, String title, Runnable back, View trailing) {
        LinearLayout header = row(); pad(header,6,3); header.setMinimumHeight(dp(57));
        if (back != null) header.addView(iconButton(R.drawable.ic_back,INK,"Quay lại",back)); else gap(header,12);
        weight(header,text(title,22,INK,true)); if(trailing!=null) header.addView(trailing);
        add(root,header); return header;
    }

    /** Large title of a tab root, such as "Lịch học" or "Lớp đã mở". */
    public LinearLayout title(LinearLayout root, String title, View trailing) {
        LinearLayout header = row(); header.setPadding(dp(18),dp(8),dp(12),dp(8)); header.setMinimumHeight(dp(60));
        weight(header,text(title,26,INK,true)); if(trailing!=null) header.addView(trailing);
        add(root,header); return header;
    }

    public LinearLayout footer(LinearLayout root) {
        LinearLayout footer = row(); footer.setPadding(dp(16),dp(6),dp(16),dp(12)); add(root,footer); return footer;
    }

    public EditText input(String hint) {
        EditText input = new EditText(context); input.setTextSize(14); input.setTextColor(INK);
        input.setHintTextColor(MUTED); input.setHint(hint); input.setSingleLine(true);
        input.setMinimumHeight(dp(48)); pad(input,13,10); surface(input,WHITE,9,BORDER);
        return input;
    }

    public LinearLayout option(String label, int icon, boolean selected, Runnable action) {
        LinearLayout view = row(); view.setPadding(dp(16),0,dp(8),0); view.setMinimumHeight(dp(47));
        surface(view,selected?PALE:WHITE,8,selected?BLUE:BORDER);
        view.addView(icon(icon,25,selected?BLUE:INK)); gap(view,11);
        TextView text = text(label,16,selected?BLUE:INK,false); text.setMaxLines(1); weight(view,text);
        view.setSelected(selected); view.setContentDescription(label+(selected?", đã chọn":""));
        clickable(view,action); return view;
    }

    // Components of the Figma file (icons are the exported vectors in res/drawable, tinted per use).

    /** Scrolling content with the 16dp side margins of the design. */
    public LinearLayout page(LinearLayout root) {
        ScrollView scroll = scroll(root); LinearLayout body = column(); body.setPadding(dp(16),dp(6),dp(16),dp(20)); scroll.addView(body); return body;
    }
    public LinearLayout bordered(int radius) { LinearLayout card = column(); surface(card,WHITE,radius,BORDER); return card; }
    public TextView lessonStatus(Lesson lesson) {
        String label = lesson.statusLabel();
        if (lesson.pending() || lesson.needsConfirmation()) return pill(label,14,ORANGE,ORANGE_BG);
        if (lesson.confirmed()) return pill(label,14,GREEN,GREEN_BG);
        return pill(label,14,MUTED,0xFFEEF2F7);
    }

    public ImageView icon(int drawable, int size, int color) {
        ImageView view = new ImageView(context); view.setImageResource(drawable);
        view.setImageTintList(ColorStateList.valueOf(color)); view.setLayoutParams(lp(size,size));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); return view;
    }
    public ImageView art(int drawable, int width, int height) {
        ImageView view = new ImageView(context); view.setImageResource(drawable); view.setLayoutParams(lp(width,height));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); return view;
    }
    /** drawable 0 (a catalog tutor without a bundled photo) shows the user icon on the pale surface instead. */
    public ImageView photo(int drawable, String description, int width, int height, int radius) {
        ImageView view = new ImageView(context); view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (drawable == 0) {
            view.setImageResource(R.drawable.ic_user); view.setImageTintList(ColorStateList.valueOf(BLUE));
            view.setScaleType(ImageView.ScaleType.FIT_CENTER); int inset = dp(Math.min(width,height)/4f); view.setPadding(inset,inset,inset,inset);
        } else view.setImageResource(drawable);
        surface(view,PALE,radius,0); view.setClipToOutline(true); view.setLayoutParams(lp(width,height));
        view.setContentDescription(description); return view;
    }

    /** Button/Primary, Button/Outline, Button/Danger and Button/Disabled; the icon sits at the start as in the design. */
    public FrameLayout action(String label, int icon, int style, Runnable click) {
        FrameLayout button = new FrameLayout(context); button.setMinimumHeight(dp(46));
        int foreground = style==OUTLINE ? BLUE : style==DANGER ? RED : WHITE;
        surface(button, style==PRIMARY ? BLUE : style==DISABLED ? GRAY : WHITE, 10, style==OUTLINE ? BLUE : style==DANGER ? RED : 0);
        TextView text = text(label,18,foreground,true); text.setGravity(Gravity.CENTER); text.setMaxLines(1);
        text.setPadding(dp(icon!=0 ? 36 : 8),0,dp(8),0);
        button.addView(text,new FrameLayout.LayoutParams(-1,-1));
        if (icon != 0) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.START|Gravity.CENTER_VERTICAL);
            params.leftMargin = dp(18); button.addView(icon(icon,22,foreground),params);
        }
        button.setContentDescription(label); text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (style == DISABLED || click == null) button.setEnabled(false); else clickable(button,click);
        return button;
    }
    public void addAction(LinearLayout parent, View button, int height) { parent.addView(button,lp(-1,height)); }
    public void weightAction(LinearLayout row, View button, int height) { row.addView(button,new LinearLayout.LayoutParams(0,dp(height),1)); }

    /** Nhãn (status pill), e.g. green "Đã xác nhận" or orange "Chờ duyệt". */
    public TextView pill(String label, int size, int color, int fill) {
        TextView view = text(label,size,color,false); view.setGravity(Gravity.CENTER); view.setMaxLines(1);
        view.setPadding(dp(10),dp(4),dp(10),dp(4)); surface(view,fill,10,0); return view;
    }
    public TextView link(String label, int size, Runnable click) {
        TextView view = text(label,size,BLUE,false); view.setGravity(Gravity.CENTER_VERTICAL); view.setMinHeight(dp(40));
        clickable(view,click); return view;
    }

    /** Trường: a bold label above an input or picker. */
    public void label(LinearLayout parent, String label) { add(parent,text(label,16,INK,true)); space(parent,7); }
    public TextView section(LinearLayout parent, String title, int size) { TextView view = text(title,size,INK,true); add(parent,view); return view; }

    /** Ô nhập acting as a picker: optional icon, value and the down chevron. */
    public LinearLayout picker(int icon, TextView value, Runnable click) {
        LinearLayout box = row(); box.setPadding(dp(12),0,dp(12),0); box.setMinimumHeight(dp(40)); surface(box,WHITE,10,BORDER);
        if (icon != 0) { box.addView(icon(icon,22,INK)); gap(box,10); }
        weight(box,value); box.addView(icon(R.drawable.ic_down,18,INK));
        clickable(box,click); return box;
    }
    public TextView value(String value) { TextView view = text(value,16,INK,false); view.setMaxLines(2); return view; }

    public EditText entry(String hint, boolean multiline) {
        EditText input = new EditText(context); input.setTextSize(16); input.setTextColor(INK); input.setHintTextColor(MUTED);
        input.setHint(hint); input.setTypeface(Typeface.create("sans-serif-condensed",Typeface.NORMAL));
        input.setSingleLine(!multiline); if (multiline) { input.setMinLines(2); input.setGravity(Gravity.TOP|Gravity.START); }
        input.setMinimumHeight(dp(multiline ? 61 : 44)); input.setPadding(dp(12),dp(10),dp(12),dp(10)); surface(input,WHITE,10,BORDER);
        input.setContentDescription(hint); return input;
    }

    /** Ghi chú: tinted info box with an icon and one or two lines of text. */
    public LinearLayout note(int icon, String message, int fill, int iconColor, int textColor) {
        LinearLayout box = row(); box.setPadding(dp(12),dp(10),dp(12),dp(10)); box.setMinimumHeight(dp(42)); surface(box,fill,10,0);
        box.addView(icon(icon,22,iconColor)); gap(box,13); weight(box,text(message,14,textColor,false)); return box;
    }

    /** Thông tin chi tiết: bordered list of icon, label and right-aligned value rows. */
    public LinearLayout table() { LinearLayout table = column(); table.setPadding(dp(11),dp(3),dp(14),dp(3)); surface(table,WHITE,10,BORDER); return table; }
    public void tableRow(LinearLayout table, int icon, String label, String value) {
        if (table.getChildCount() > 0) {
            View divider = new View(context); divider.setBackgroundColor(BORDER);
            LinearLayout.LayoutParams params = lp(-1,1); params.leftMargin = dp(32); table.addView(divider,params);
        }
        LinearLayout row = row(); row.setMinimumHeight(dp(38)); row.addView(icon(icon,21,INK)); gap(row,12);
        weight(row,text(label,15,MUTED,false)); TextView right = text(value,15,INK,false); right.setGravity(Gravity.END);
        row.addView(right); add(table,row);
    }
    /** Icon followed by a single line of text, used inside cards. */
    public LinearLayout fact(int icon, String value, int iconSize, int size, int color, boolean bold) {
        LinearLayout row = row(); row.addView(icon(icon,iconSize,color==ORANGE?ORANGE:INK)); gap(row,8);
        weight(row,text(value,size,color,bold)); row.setMinimumHeight(dp(27)); return row;
    }

    /** Chọn: selectable cell such as a time slot or the week/month toggle. */
    public TextView choice(String label, boolean selected, Runnable click) {
        TextView view = text(label,16,selected?WHITE:INK,selected); view.setGravity(Gravity.CENTER); view.setMinHeight(dp(38));
        surface(view,selected?BLUE:WHITE,9,selected?BLUE:BORDER); view.setSelected(selected);
        view.setContentDescription(label+(selected?", đã chọn":"")); clickable(view,click); return view;
    }
    /** Day cell of the date strip (weekday label above the day number). */
    public LinearLayout day(java.time.LocalDate date, boolean selected, Runnable click) {
        LinearLayout cell = column(); cell.setGravity(Gravity.CENTER); cell.setMinimumHeight(dp(61));
        surface(cell,selected?BLUE:WHITE,9,selected?BLUE:BORDER);
        int weekday = date.getDayOfWeek().getValue();
        cell.addView(text(weekday==7?"CN":"T"+(weekday+1),12,selected?WHITE:MUTED,false)); space(cell,6);
        cell.addView(text(String.valueOf(date.getDayOfMonth()),19,selected?WHITE:INK,true));
        cell.setSelected(selected); cell.setContentDescription("Ngày "+date.getDayOfMonth()+" tháng "+date.getMonthValue()+(selected?", đã chọn":""));
        clickable(cell,click); return cell;
    }

    /** Tab row with the blue underline under the selected tab. */
    public LinearLayout tabs(LinearLayout root, String[] labels, int selected, java.util.function.IntConsumer select) {
        LinearLayout tabs = row();
        for (int i = 0; i < labels.length; i++) {
            int index = i; boolean active = i == selected;
            LinearLayout tab = column(); TextView label = text(labels[i],16,active?BLUE:MUTED,active);
            label.setGravity(Gravity.CENTER); label.setMinHeight(dp(43)); label.setMaxLines(1); add(tab,label);
            View underline = new View(context); surface(underline,active?BLUE:WHITE,2,0);
            LinearLayout.LayoutParams under = lp(-1,3); under.setMargins(dp(8),0,dp(8),0); tab.addView(underline,under);
            tab.setSelected(active); tab.setContentDescription(labels[i]+(active?", đang chọn":""));
            clickable(tab,()->select.accept(index)); weight(tabs,tab);
        }
        add(root,tabs); line(root); return tabs;
    }

    public CheckBox check(String label, boolean checked, CompoundButton.OnCheckedChangeListener change) {
        CheckBox box = new CheckBox(context); box.setText(label); box.setTextSize(15); box.setTextColor(INK); box.setChecked(checked);
        box.setTypeface(Typeface.create("sans-serif-condensed",Typeface.NORMAL)); box.setButtonTintList(ColorStateList.valueOf(BLUE));
        box.setMinHeight(dp(44)); box.setOnCheckedChangeListener(change); return box;
    }
    /** Lựa chọn (radio row); the selected row gets the pale surface of the design. */
    public LinearLayout radio(String label, boolean selected, Runnable click) {
        LinearLayout row = row(); row.setPadding(dp(14),0,dp(12),0); row.setMinimumHeight(dp(44)); surface(row,selected?PALE:WHITE,9,0);
        FrameLayout mark = new FrameLayout(context); surface(mark,WHITE,15,selected?BLUE:INK);
        if (selected) { View dot = new View(context); surface(dot,BLUE,8,0); mark.addView(dot,new FrameLayout.LayoutParams(dp(11),dp(11),Gravity.CENTER)); }
        row.addView(mark,lp(21,21)); gap(row,18); weight(row,text(label,19,INK,false));
        row.setSelected(selected); row.setContentDescription(label+(selected?", đã chọn":", chưa chọn")); clickable(row,click); return row;
    }
}
