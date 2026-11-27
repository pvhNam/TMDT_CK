package com.example.tmdt.ui.groupclass;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.ThemeColors;
import com.example.tmdt.Classroom;
import com.example.tmdt.Lesson;
import com.example.tmdt.Tutor;
import com.example.tmdt.GroupClass;
import com.example.tmdt.GroupMatch;

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
        ui.title(root,"Lớp học nhóm",ui.iconButton(R.drawable.ic_bell,Ui.INK,"Thông báo",classroom::showNotifications));
        LinearLayout body = ui.page(root);
        LinearLayout box = ui.row(); box.setPadding(ui.dp(12),0,ui.dp(6),0); ui.fieldSurface(box,Ui.PALE,Ui.BORDER);
        box.addView(ui.icon(R.drawable.ic_search,22,Ui.MUTED));
        search = ui.entry("Tìm lớp, môn học...",false); search.setBackgroundColor(Color.TRANSPARENT);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,id,event)->{classroom.hideKeyboard();return true;});
        ui.weight(box,search); ui.add(body,box); ui.space(body,13);
        filters = ui.row(); ui.add(body,filters); ui.space(body,16);
        results = ui.column(); results.setClipChildren(false); ui.add(body,results);
        body.setClipChildren(false); body.setClipToPadding(false);
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
    @Override protected boolean updateContent() { if(results==null)return false;renderResults();return true; }

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
        StableViews rows=StableViews.at(results);rows.begin();
        String query = normalized(search.getText().toString());
        // Ghép nhóm có cùng yêu cầu: classes that suit the student's own tutor requests come first while nothing is searched.
        Map<GroupClass,GroupMatch.Request> suggestions = query.isEmpty() && subject.isEmpty() && mode.isEmpty() && !seats ? classroom.suggestions() : new HashMap<>();
        if (!suggestions.isEmpty()) {
            rows.add("suggestions",true,()->ui.text("Phù hợp với tin đăng của bạn",18,Ui.INK,true),ui.dp(10));
            for (Map.Entry<GroupClass,GroupMatch.Request> entry : suggestions.entrySet())
                rows.add("suggestion/"+entry.getKey().id,java.util.Arrays.asList(entry.getKey(),entry.getValue()),()->card(entry.getKey(),entry.getValue()),ui.dp(13));
            rows.add("all",true,()->ui.text("Tất cả lớp",18,Ui.INK,true),ui.dp(10));
        }
        List<GroupClass> found = new ArrayList<>();
        for (GroupClass item : classroom.store.classes) {
            if (!listed(item)) continue;
            if (!subject.isEmpty() && !item.subject.equals(subject)) continue;
            if (!mode.isEmpty() && !item.mode.equals(mode)) continue;
            if (seats && !item.recruiting()) continue;
            if (!normalized(item.title+" "+item.subject+" "+item.level+" "+item.tutorName).contains(query)) continue;
            found.add(item);
        }
        found.sort(Comparator.comparing(GroupClass::started).thenComparing((GroupClass item) -> item.startDate).thenComparingInt(GroupClass::begin));
        for (GroupClass item : found) rows.add("class/"+item.id,item,()->card(item,null),ui.dp(13));
        boolean filtered = !query.isEmpty() || !subject.isEmpty() || !mode.isEmpty() || seats;
        if (found.isEmpty()) rows.add("empty/"+filtered,true,()->filtered
                ? classroom.emptyState(R.drawable.ic_search,"Không tìm thấy lớp phù hợp","Thử từ khóa khác hoặc bỏ bớt bộ lọc.","Xóa bộ lọc",this::clearFilters)
                : classroom.emptyState(R.drawable.ic_users,"Chưa có lớp đang tuyển sinh","Khi gia sư mở lớp mới, lớp sẽ hiện ở đây.",null,null),ui.dp(13));
        rows.commit();
    }

    /** Classes still taking students, plus the ones the student already joined or applied to; never the user's own classes. */
    private boolean listed(GroupClass item) {
        if (!item.active() || item.tutorId.equals(classroom.me())) return false;
        return item.attends(classroom.me()) || (GroupClass.OPEN.equals(item.status) && !item.started());
    }
    private void clearFilters() { subject = ""; mode = ""; seats = false; search.setText(""); renderFilters(); renderResults(); }

    @Override public void onDestroyView(){super.onDestroyView();filters=null;results=null;search=null;}

    /** A class card; a suggested one names the student's request it suits. The whole card opens the class. */
    private View card(GroupClass item, GroupMatch.Request request) {
        LinearLayout card = ui.column(); ui.pad(card,14,14); raise(ui,card,3);
        if (request != null) {
            TextView match = ui.pill("Khớp tin đăng \""+request.title+"\"",13,Ui.GREEN,Ui.GREEN_BG);
            match.setEllipsize(TextUtils.TruncateAt.END); card.addView(match,ui.lp(-2,-2)); ui.space(card,12);
        }
        LinearLayout top = ui.row(); top.setGravity(Gravity.TOP);
        top.addView(ui.photo(item.art(),null,84,84,18)); ui.gap(top,12);
        LinearLayout info = ui.column();
        TextView title = ui.text(item.title,18,Ui.INK,true); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END); ui.add(info,title); ui.space(info,6);
        LinearLayout teacher = ui.row(); teacher.addView(ui.avatar(item.tutorId,item.tutorName,24)); ui.gap(teacher,8);
        ui.weight(teacher,line(ui.text(item.tutorName,14,Ui.MUTED,false))); ui.add(info,teacher); ui.space(info,8);
        info.addView(availability(item),ui.lp(-2,-2)); ui.weight(top,info); ui.add(card,top); ui.space(card,12);
        ui.add(card,ui.fact(R.drawable.ic_calendar,"Khai giảng "+item.startLabel()+" · "+item.scheduleLabel(),18,14,Ui.INK,false));
        ui.add(card,ui.fact(item.online()?R.drawable.ic_video:R.drawable.ic_home_soft,item.sessions+" buổi · "+item.mode,18,14,Ui.INK,false));
        ui.space(card,10); ui.line(card); ui.space(card,12);
        LinearLayout bottom = ui.row(); LinearLayout price = ui.column();
        ui.add(price,line(ui.text(Tutor.money(item.price),17,Ui.BLUE,true))); ui.space(price,2);
        ui.add(price,line(ui.text(item.perSession?"mỗi buổi":"trọn khóa",12,Ui.MUTED,false))); ui.weight(bottom,price);
        FrameLayout ask = new FrameLayout(requireContext()); ui.surface(ask,Ui.PALE,22,0);
        ask.addView(ui.icon(R.drawable.ic_chat,20,Ui.BLUE),new FrameLayout.LayoutParams(ui.dp(20),ui.dp(20),Gravity.CENTER));
        ask.setContentDescription(getString(R.string.class_questions_title)); ui.clickable(ask,()->ClassQuestionsDialog.open(this,item.id));
        bottom.addView(ask,ui.lp(44,44)); ui.gap(bottom,8);
        View open = ui.action("Xem lớp",0,Ui.PRIMARY,()->open(classroom,item)); open.setMinimumHeight(0);
        open.setElevation(ui.dp(4)); open.setOutlineAmbientShadowColor(0x550866F5); open.setOutlineSpotShadowColor(0x770866F5);
        bottom.addView(open,ui.lp(108,44)); ui.add(card,bottom);
        ui.clickable(card,()->open(classroom,item)); card.setContentDescription(item.title+", "+item.tutorName);
        return card;
    }

    /** Up to five classes still taking students that start from today, soonest first, for the student home screen. */
    public static void upcoming(Classroom classroom, LinearLayout parent) {
        Ui ui = classroom.ui; String today = LocalDate.now().toString();
        List<GroupClass> list = new ArrayList<>();
        for (GroupClass item : classroom.store.classes)
            if (GroupClass.OPEN.equals(item.status) && item.startDate != null && item.startDate.compareTo(today) >= 0) list.add(item);
        list.sort(Comparator.comparing((GroupClass item) -> item.startDate).thenComparingInt(GroupClass::begin));
        StableViews rows = StableViews.at(parent); rows.begin();
        rows.add("heading",true,()->{
            LinearLayout heading = ui.row();
            ui.weight(heading,ui.text("Lớp học nhóm sắp khai giảng",18,Ui.INK,true)); ui.gap(heading,8);
            heading.addView(ui.link("Xem thêm ›",14,()->classroom.show("groups")));
            return heading;
        },ui.dp(6));
        if (list.isEmpty()) rows.add("empty",true,()->{
            LinearLayout empty = ui.row(); ui.pad(empty,14,14); raise(ui,empty,2);
            empty.addView(ui.icon(R.drawable.ic_users,24,Ui.BLUE)); ui.gap(empty,12);
            ui.weight(empty,ui.text("Chưa có lớp nào sắp khai giảng. Bấm Xem thêm để tìm lớp học nhóm.",14,Ui.MUTED,false));
            ui.clickable(empty,()->classroom.show("groups")); return empty;
        },ui.dp(4));
        for (GroupClass item : list.subList(0,Math.min(5,list.size())))
            rows.add("class/"+item.id,Arrays.asList(item,classroom.me()),()->row(classroom,item),ui.dp(10));
        rows.commit();
    }

    /** A compact card: photo, title, tutor, start date and time, price and seats. */
    private static View row(Classroom classroom, GroupClass item) {
        Ui ui = classroom.ui;
        LinearLayout card = ui.row(); card.setPadding(ui.dp(10),ui.dp(10),ui.dp(12),ui.dp(10)); raise(ui,card,2);
        card.addView(ui.photo(item.art(),null,76,76,16)); ui.gap(card,12);
        LinearLayout info = ui.column();
        ui.add(info,line(ui.text(item.title,16,Ui.INK,true))); ui.space(info,3);
        ui.add(info,line(ui.text(item.tutorName+" · "+item.subject+" "+item.level,13,Ui.MUTED,false))); ui.space(info,5);
        LinearLayout when = ui.row(); when.addView(ui.icon(R.drawable.ic_calendar,15,Ui.BLUE)); ui.gap(when,6);
        ui.weight(when,line(ui.text("Khai giảng "+LocalDate.parse(item.startDate).format(DAY)+" · "+Lesson.range(item.hour,item.minute,item.minutes),13,Ui.INK,false)));
        ui.add(info,when); ui.space(info,5);
        LinearLayout bottom = ui.row();
        ui.weight(bottom,line(ui.text(item.priceLabel(),14,Ui.BLUE,true))); ui.gap(bottom,6);
        boolean full = item.full();
        bottom.addView(ui.text(full?"Đã đủ chỗ":"Còn "+item.seatsLeft()+" chỗ",12,full?Ui.ORANGE:Ui.GREEN,true));
        ui.add(info,bottom); ui.weight(card,info);
        ui.clickable(card,()->open(classroom,item)); card.setContentDescription(item.title+", "+item.tutorName);
        return card;
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static void open(Classroom classroom, GroupClass item) { classroom.openClass(item,item.full()?"full":"join"); }
    private static TextView line(TextView view) { view.setMaxLines(1); view.setEllipsize(TextUtils.TruncateAt.END); return view; }
    /** A white card with a soft blue shadow instead of a border; dark mode keeps the border since a shadow does not show there. */
    private static void raise(Ui ui, View view, int elevation) {
        ui.cardSurface(view,Ui.WHITE,ThemeColors.dark(ui.context)?Ui.BORDER:0);
        view.setElevation(ui.dp(elevation)); view.setOutlineAmbientShadowColor(0x226583AD); view.setOutlineSpotShadowColor(0x446583AD);
    }

    private TextView availability(GroupClass item) {
        if (item.members.contains(classroom.me())) return ui.pill("Đã tham gia",14,Ui.GREEN,Ui.GREEN_BG);
        if (item.registrationOf(classroom.me()) != null) return ui.pill("Đã gửi đăng ký",14,Ui.BLUE,Ui.PALE);
        if (item.full()) return ui.pill("Đã đủ "+item.capacity+"/"+item.capacity+" chỗ",14,Ui.ORANGE,Ui.ORANGE_BG);
        if (item.started()) return ui.pill("Đã khai giảng",14,Ui.MUTED,0xFFEEF2F7);
        if (!GroupClass.OPEN.equals(item.status)) return ui.pill("Đã đóng tuyển sinh",14,Ui.MUTED,0xFFEEF2F7);
        return ui.pill("Còn "+item.seatsLeft()+"/"+item.capacity+" chỗ",14,Ui.GREEN,Ui.GREEN_BG);
    }
}
