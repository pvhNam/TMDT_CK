package com.example.tmdt.ui.groupclass;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.GroupClass;

/** Screen 37 · Tìm lớp học nhóm (UC19): search and filter open group classes. */
public final class GroupSearchFragment extends ScreenFragment {
    private LinearLayout filters, results;
    private EditText search;
    private static final String[] MODES = {"Mọi hình thức","Trực tuyến","Tại nhà"};
    /** Empty subject or mode means no filter. */
    private String subject = "", mode = "";
    private boolean seats;

    @Override protected View build() {
        // "Tìm lớp tương tự" on screen 60 opens this list filtered to the same subject and to classes that still have seats.
        if (classroom.similarSubject != null) { subject = classroom.similarSubject; seats = true; classroom.similarSubject = null; }
        LinearLayout root = ui.column();
        ui.title(root,"Lớp học nhóm",ui.iconButton(R.drawable.ic_bell,Ui.INK,"Thông báo",
                ()->classroom.dialog("Thông báo","Chức năng này hiện chưa khả dụng.")));
        LinearLayout body = ui.page(root);
        LinearLayout box = ui.row(); box.setPadding(ui.dp(12),0,ui.dp(6),0); ui.fieldSurface(box,Ui.PALE,Ui.BORDER);
        box.addView(ui.icon(R.drawable.ic_search,22,Ui.MUTED));
        search = ui.entry("Tìm lớp, môn học...",false); search.setBackgroundColor(Color.TRANSPARENT);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,id,event)->{classroom.hideKeyboard();return true;});
        ui.weight(box,search); ui.add(body,box); ui.space(body,13);
        filters = ui.row(); ui.add(body,filters); ui.space(body,13);
        results = ui.column(); ui.add(body,results);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { renderResults(); }
            public void afterTextChanged(Editable s) {}
        });
        renderFilters(); renderResults();
        return root;
    }

    /** Subject and mode open a list to choose from (UC19); "Còn chỗ" switches on and off. */
    /** New data only redraws the results, so the search text and filters stay as they are. */
    @Override public void refresh() { if (results != null && getView() != null) renderResults(); else super.refresh(); }

    private void renderFilters() {
        filters.removeAllViews();
        chip("∑  "+(subject.isEmpty()?"Môn học":subject)+" ⌄","môn học: "+(subject.isEmpty()?"tất cả":subject),!subject.isEmpty(),this::pickSubject);
        ui.gap(filters,7);
        chip("▣  "+(mode.isEmpty()?"Hình thức":mode)+" ⌄","hình thức: "+(mode.isEmpty()?"tất cả":mode),!mode.isEmpty(),this::pickMode);
        ui.gap(filters,7);
        chip("♟  Còn chỗ","còn chỗ",seats,()->{seats=!seats;renderFilters();renderResults();});
    }
    private void chip(String label, String description, boolean selected, Runnable click) {
        TextView chip = ui.pill(label,14,selected?Ui.WHITE:Ui.BLUE,selected?Ui.BLUE:Ui.PALE); chip.setMinHeight(ui.dp(41));
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setSelected(selected); chip.setContentDescription("Lọc "+description+(selected?", đang bật":""));
        ui.clickable(chip,click);
        ui.weight(filters,chip);
    }
    private void pickSubject() {
        List<String> names = classroom.subjects();
        String[] items = new String[names.size()+1]; items[0] = "Tất cả môn";
        for (int i = 0; i < names.size(); i++) items[i+1] = names.get(i);
        choose("Lọc theo môn học",items,names.indexOf(subject)+1,i->subject=i==0?"":names.get(i-1));
    }
    private void pickMode() {
        choose("Lọc theo hình thức",MODES,Math.max(0,Arrays.asList(MODES).indexOf(mode)),i->mode=i==0?"":MODES[i]);
    }
    private void choose(String title, String[] items, int selected, IntConsumer apply) {
        classroom.ui.dialog().setTitle(title)
                .setSingleChoiceItems(items,selected,(dialog,which)->{apply.accept(which);renderFilters();renderResults();dialog.dismiss();})
                .setNegativeButton("Đóng",null).show();
    }

    private static String normalized(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('đ','d').trim();
    }

    private void renderResults() {
        results.removeAllViews(); String query = normalized(search.getText().toString()); int count = 0;
        for (GroupClass item : classroom.store.classes) {
            if (GroupClass.ENDED.equals(item.status)) continue;
            if (!subject.isEmpty() && !item.subject.equals(subject)) continue;
            if (!mode.isEmpty() && !item.mode.equals(mode)) continue;
            if (seats && !item.recruiting()) continue;
            if (!normalized(item.title+" "+item.subject+" "+item.level+" "+item.tutorName).contains(query)) continue;
            ui.add(results,card(item)); ui.space(results,13); count++;
        }
        if (count == 0) {
            LinearLayout empty = ui.column(); empty.setGravity(Gravity.CENTER); empty.setPadding(ui.dp(18),ui.dp(28),ui.dp(18),ui.dp(22)); ui.cardSurface(empty,Ui.PALE,0);
            empty.addView(ui.icon(R.drawable.ic_users,40,Ui.BLUE)); ui.space(empty,14);
            TextView title = ui.text("Chưa có lớp phù hợp",18,Ui.INK,true); title.setGravity(Gravity.CENTER); ui.add(empty,title); ui.space(empty,8);
            TextView hint = ui.text("Thử từ khóa khác hoặc bỏ bớt bộ lọc.",15,Ui.MUTED,false); hint.setGravity(Gravity.CENTER); ui.add(empty,hint); ui.space(empty,16);
            ui.addAction(empty,ui.action("Xóa bộ lọc",0,Ui.OUTLINE,()->{subject=mode="";seats=false;search.setText("");renderFilters();renderResults();}),44);
            ui.add(results,empty);
        }
    }

    private View card(GroupClass item) {
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(9),ui.dp(12),ui.dp(10),ui.dp(10));
        LinearLayout top = ui.row(); top.setGravity(Gravity.TOP);
        top.addView(ui.art(item.largeArt(),99,133)); ui.gap(top,13);
        LinearLayout info = ui.column(); ui.add(info,ui.text(item.title,22,Ui.INK,true)); ui.space(info,6);
        LinearLayout teacher = ui.row(); teacher.addView(ui.photo(0,"Ảnh gia sư "+item.tutorName,43,44,9)); ui.gap(teacher,12);
        ui.weight(teacher,ui.text(item.tutorName,16,Ui.INK,false)); ui.add(info,teacher); ui.space(info,6);
        ui.add(info,ui.fact(item.mode.equals("Tại nhà")?R.drawable.ic_home:R.drawable.ic_video,item.sessions+" buổi · "+item.mode,19,16,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_calendar,item.scheduleLabel(),19,16,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_coin,item.priceLabel(),19,16,Ui.INK,true)); ui.space(info,3);
        info.addView(availability(item),ui.lp(-2,28)); ui.weight(top,info); ui.add(card,top); ui.space(card,8);
        ui.addAction(card,ui.action("Xem lớp",0,Ui.OUTLINE,()->classroom.openClass(item,item.full()?"full":"join")),41);
        return card;
    }

    private TextView availability(GroupClass item) {
        if (item.members.contains(classroom.me())) return ui.pill("Đã tham gia",14,Ui.GREEN,Ui.GREEN_BG);
        if (item.registrationOf(classroom.me()) != null) return ui.pill("Đã gửi đăng ký",14,Ui.BLUE,Ui.PALE);
        if (item.full()) return ui.pill("Đã đủ "+item.capacity+"/"+item.capacity+" chỗ",14,Ui.ORANGE,Ui.ORANGE_BG);
        if (!GroupClass.OPEN.equals(item.status)) return ui.pill("Đã đóng tuyển sinh",14,Ui.MUTED,0xFFEEF2F7);
        return ui.pill("Còn "+item.seatsLeft()+"/"+item.capacity+" chỗ",14,Ui.GREEN,Ui.GREEN_BG);
    }
}
