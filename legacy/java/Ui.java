package com.example.tmdt;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared spacing, typography and components for the four native screens. */
final class Ui {
    static final int INK = 0xFF081F50, MUTED = 0xFF4D6287, BLUE = 0xFF0866F5,
            PALE = 0xFFEAF6FF, BORDER = 0xFFDCE8F7, WHITE = 0xFFFFFFFF,
            GREEN = 0xFF13834E, GOLD = 0xFFFFB02D;
    final Context context;

    Ui(Context context) { this.context = context; }
    int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    LinearLayout column() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    LinearLayout row() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    void pad(View view, int horizontal, int vertical) { view.setPadding(dp(horizontal),dp(vertical),dp(horizontal),dp(vertical)); }
    LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }
    void add(LinearLayout parent, View child) { parent.addView(child, lp(-1,-2)); }
    void space(LinearLayout parent, int height) { parent.addView(new View(context),lp(1,height)); }
    void gap(LinearLayout parent, int width) { parent.addView(new View(context),lp(width,1)); }
    void weight(LinearLayout row, View child) { row.addView(child,new LinearLayout.LayoutParams(0,-2,1)); }
    void line(LinearLayout parent) {
        View view = new View(context); view.setBackgroundColor(BORDER); parent.addView(view,lp(-1,1));
    }

    TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setFontFeatureSettings("kern"); view.setIncludeFontPadding(false);
        view.setLayoutParams(lp(-2,-2));
        view.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setLineSpacing(dp(3),1);
        return view;
    }

    void heading(LinearLayout parent, String title) { add(parent,text(title,16,INK,true)); space(parent,9); }
    GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color);
        drawable.setCornerRadius(dp(radius)); if (stroke != 0) drawable.setStroke(dp(1),stroke);
        return drawable;
    }
    void surface(View view, int color, int radius, int stroke) { view.setBackground(background(color,radius,stroke)); }
    void clickable(View view, Runnable action) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x220866F5),view.getBackground(),background(WHITE,12,0)));
        view.setOnClickListener(v -> action.run()); view.setFocusable(true);
    }

    LinearLayout card() { LinearLayout card = column(); pad(card,12,12); surface(card,WHITE,12,BORDER); return card; }
    ScrollView scroll(LinearLayout parent) {
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false); parent.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); return scroll;
    }
    LinearLayout body(LinearLayout root) {
        ScrollView scroll = scroll(root); LinearLayout body = column(); pad(body,18,12); scroll.addView(body); return body;
    }

    LinearLayout button(String title, String icon, boolean primary, Runnable action) {
        LinearLayout button = row(); button.setGravity(Gravity.CENTER); button.setMinimumHeight(dp(50)); pad(button,10,9);
        surface(button,primary?BLUE:WHITE,10,primary?0:BLUE);
        if (icon != null) { button.addView(new LineIcon(context,icon,primary?WHITE:BLUE),lp(22,22)); gap(button,8); }
        TextView label = text(title,14,primary?WHITE:BLUE,true); label.setGravity(Gravity.CENTER);
        button.addView(label,lp(-2,-2)); button.setContentDescription(title);
        button.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        for(int i=0;i<button.getChildCount();i++) button.getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        clickable(button,action); return button;
    }

    View iconButton(String icon, String description, Runnable action) {
        LinearLayout button = row(); button.setGravity(Gravity.CENTER);
        button.addView(new LineIcon(context,icon,INK),lp(23,23)); button.setContentDescription(description);
        clickable(button,action); button.setLayoutParams(lp(48,48)); return button;
    }

    LinearLayout header(LinearLayout root, String title, Runnable back, View trailing) {
        LinearLayout header = row(); pad(header,6,3); header.setMinimumHeight(dp(57));
        if (back != null) header.addView(iconButton("back","Quay lại",back)); else gap(header,12);
        weight(header,text(title,21,INK,true)); if(trailing!=null) header.addView(trailing);
        add(root,header); return header;
    }

    LinearLayout footer(LinearLayout root) {
        line(root); LinearLayout footer = row(); pad(footer,18,12); add(root,footer); return footer;
    }

    TextView badge(String label) {
        TextView view = text(label,12,GREEN,false); pad(view,9,5); surface(view,0xFFDCF6EB,8,0); return view;
    }

    DesignImage portrait(Tutor tutor, int width, int height) {
        DesignImage image = new DesignImage(context,tutor.id); image.setLayoutParams(lp(width,height)); return image;
    }

    LinearLayout rating(Tutor tutor) {
        LinearLayout row = row(); row.addView(new LineIcon(context,"star",GOLD),lp(18,18)); gap(row,5);
        row.addView(text(tutor.rating+" ("+tutor.students+")",13,MUTED,false)); return row;
    }

    View tutorCard(Tutor tutor, Runnable action) {
        LinearLayout card = card(); LinearLayout row = row(); row.addView(portrait(tutor,72,88)); gap(row,12);
        LinearLayout details = column(); add(details,text(tutor.name,15,INK,true)); space(details,5);
        add(details,text(tutor.subject+" · "+tutor.level,13,MUTED,false)); space(details,5);
        add(details,rating(tutor)); space(details,4); add(details,text(Tutor.money(tutor.rate)+" / giờ",14,INK,true));
        weight(row,details); row.addView(new LineIcon(context,"next",INK),lp(18,18)); add(card,row);
        clickable(card,action); return card;
    }

    EditText input(String hint) {
        EditText input = new EditText(context); input.setTextSize(14); input.setTextColor(INK);
        input.setHintTextColor(MUTED); input.setHint(hint); input.setSingleLine(true);
        input.setMinimumHeight(dp(48)); pad(input,13,10); surface(input,WHITE,9,BORDER);
        return input;
    }

    LinearLayout option(String label, String icon, boolean selected, Runnable action) {
        LinearLayout view = button(label,icon,false,action);
        surface(view,selected?PALE:WHITE,9,selected?BLUE:BORDER);
        for(int i=0;i<view.getChildCount();i++) if(view.getChildAt(i) instanceof TextView)
            ((TextView)view.getChildAt(i)).setTextColor(selected?BLUE:INK);
        view.setSelected(selected); view.setContentDescription(label+(selected?", đã chọn":""));
        return view;
    }
}
