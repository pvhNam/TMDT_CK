package com.example.tmdt.ui.schedule;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.Motion;
import com.example.tmdt.ui.common.ThemeColors;
import com.google.android.material.tabs.TabLayout;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Lesson;

/** Screen 04 · Lịch học (UC24): the student's upcoming, pending and past lessons. */
public final class ScheduleFragment extends ScreenFragment {
    private final ScheduleCards<View> cards=new ScheduleCards<>();
    private LinearLayout body,list;
    private LinearLayout history;
    private TextView tutorSwitch;
    private TabLayout tabs;
    private ScrollView scroll;
    private int renderedTab=-1;
    /** Each tab lists its cards three to a page, swiped left and right inside one frame. */
    private static final int PER_PAGE=3;
    private final Pages pages=new Pages();
    private LinearLayout frame,bar,dots;
    private ViewPager2 pager;
    private View previous,following;

    @Override protected View build() {
        LinearLayout root=ui.column();
        tutorSwitch=ui.link("Chế độ gia sư",15,this::tutorMode);ui.title(root,"Lịch học",tutorSwitch);
        tabs=new TabLayout(requireContext());tabs.setTabMode(TabLayout.MODE_FIXED);tabs.setTabGravity(TabLayout.GRAVITY_FILL);
        tabs.setTabTextColors(requireContext().getColor(R.color.app_muted),requireContext().getColor(R.color.app_accent));
        tabs.setSelectedTabIndicatorColor(requireContext().getColor(R.color.app_accent));tabs.setSelectedTabIndicatorHeight(ui.dp(3));
        for(String label:new String[]{"Sắp tới","Chờ xác nhận","Đã học"})tabs.addTab(tabs.newTab().setText(label),false);
        tabs.getTabAt(classroom.scheduleTab).select();ui.add(root,tabs);ui.line(root);
        body=ui.page(root);scroll=(ScrollView)body.getParent();ui.space(body,6);
        history=ui.column();ui.add(body,history);ui.space(body,12);
        list=ui.column();ui.add(body,list);frame=pagerFrame();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override public void onTabSelected(TabLayout.Tab tab){
                if(classroom.scheduleTab==tab.getPosition())return;
                classroom.scheduleTab=tab.getPosition();render();scroll.scrollTo(0,0);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab){}
            @Override public void onTabReselected(TabLayout.Tab tab){}
        });
        render();return root;
    }

    @Override protected boolean updateContent(){
        if(list==null)return false;
        if(tabs.getSelectedTabPosition()!=classroom.scheduleTab)tabs.getTabAt(classroom.scheduleTab).select();
        render();return true;
    }

    private void render(){
        tutorSwitch.setVisibility(classroom.isTutor()?View.VISIBLE:View.GONE);
        history.removeAllViews();history.addView(registrationsLink());
        int tab=classroom.scheduleTab;
        List<Entry> visible=new ArrayList<>();
        for(Lesson lesson:classroom.lessons) {
            if(!lesson.studentId.equals(classroom.me()))continue;
            boolean include=tab==1?lesson.pending()||Lesson.REJECTED.equals(lesson.status):
                    tab==2?(lesson.confirmed()&&lesson.ended())||Lesson.CANCELLED.equals(lesson.status):
                    lesson.confirmed()&&!lesson.ended();
            if(include)visible.add(new Entry("lesson/"+lesson.id,lesson.start(),
                    Arrays.asList(lesson.toMap(),lesson.statusLabel(),lesson.needsConfirmation()),()->lessonCard(lesson)));
        }
        // Group classes: registrations wait under "Chờ xác nhận"; members see the next session of each class under "Sắp tới"
        // (plus one the tutor asks to move) and every past session under "Đã học".
        for(GroupClass item:classroom.store.classes) {
            Object groupSnapshot=item.toMap();
            if(tab==1) {
                for(GroupClass.Registration registration:item.registrations) {
                    if(!registration.studentId.equals(classroom.me())||!(registration.pending()||Lesson.REJECTED.equals(registration.status)))continue;
                    boolean waiting=registration.pending();
                    String sent=LocalDate.parse(registration.date).format(Lesson.DATE);
                    // Registrations still waiting when the class stopped recruiting are closed without the tutor's answer.
                    String note=waiting?"Đăng ký gửi ngày "+sent+".":item.cancelled()?"Lớp đã bị gia sư hủy nên đăng ký gửi ngày "+sent+" không được duyệt.":GroupClass.OPEN.equals(item.status)?"Gia sư đã từ chối đăng ký gửi ngày "+sent+"."
                            :"Lớp đã đóng tuyển sinh nên đăng ký gửi ngày "+sent+" không được duyệt.";
                    visible.add(new Entry("registration/"+registration.id(),LocalDate.parse(item.startDate).atTime(item.hour,item.minute),
                            Arrays.asList(groupSnapshot,registration.toMap(),note),()->classCard(item,"Khai giảng "+item.startLabel()+" · "+item.scheduleLabel(),
                            waiting?ui.pill("Chờ duyệt",14,Ui.ORANGE,Ui.ORANGE_BG):ui.pill("Bị từ chối",14,Ui.MUTED,0xFFEEF2F7),note)));
                }
            } else if(item.members.contains(classroom.me())) {
                if(item.cancelled()) {
                    if(tab==0&&!item.over())visible.add(new Entry("cancelled/"+item.id,LocalDate.parse(item.startDate).atTime(item.hour,item.minute),
                            groupSnapshot,()->classCard(item,item.scheduleLabel(),
                            ui.pill("Đã hủy",14,Ui.RED,0xFFFFE9EC),"Gia sư đã hủy lớp. Lý do: "+item.cancelReason+".")));
                    continue;
                }
                List<GroupClass.Session> sessions=item.sessionList(),ahead=new ArrayList<>();
                for(GroupClass.Session session:sessions) {
                    if(!session.ended()){ahead.add(session);continue;}
                    if(tab!=2)continue;
                    visible.add(new Entry("session/"+item.id+"/"+session.number,session.start(),
                            Arrays.asList(groupSnapshot,session.start(),tab),()->classCard(item,session.label(),
                            ui.pill("Buổi "+session.number+"/"+item.sessions,14,Ui.MUTED,0xFFEEF2F7),"Buổi "+session.number+" trên tổng số "+item.sessions+" buổi.")));
                }
                if(tab!=0||!item.active()||ahead.isEmpty())continue;
                visible.add(upcoming(item,ahead.get(0),ahead,groupSnapshot));
                if(item.proposalOpen()&&item.proposal.number<=sessions.size()) {
                    GroupClass.Session asked=sessions.get(item.proposal.number-1);
                    if(asked!=ahead.get(0)&&!asked.ended())visible.add(upcoming(item,asked,ahead,groupSnapshot));
                }
            }
        }
        Comparator<Entry> order=Comparator.comparing((Entry entry)->entry.start).thenComparing(entry->entry.id);
        visible.sort(tab==2?order.reversed():order);
        List<View> next=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(Entry entry:visible){
            ids.add(entry.id);
            next.add(cards.get(entry.id,entry.content,()->{
                View card=entry.create.get();ThemeColors.apply(card);Motion.bindControls(card);return card;
            }));
        }
        cards.retain(ids);
        List<List<View>> split=new ArrayList<>();
        for(int i=0;i<next.size();i+=PER_PAGE)split.add(new ArrayList<>(next.subList(i,Math.min(next.size(),i+PER_PAGE))));
        boolean switched=renderedTab!=tab;
        if(!switched&&split.equals(pages.pages))return;
        pages.pages.clear();pages.pages.addAll(split);pages.notifyDataSetChanged();
        View shown=next.isEmpty()?empty(tab):frame;
        if(list.getChildCount()!=1||list.getChildAt(0)!=shown){list.removeAllViews();list.addView(shown,ui.lp(-1,-2));}
        pager.setCurrentItem(switched?0:Math.min(pager.getCurrentItem(),Math.max(0,split.size()-1)),false);
        if(switched&&renderedTab!=-1)Motion.reveal(list);
        renderedTab=tab;renderBar();fitPager();
    }

    private View empty(int tab){
        if(tab==1)return classroom.emptyState(R.drawable.ic_clock,"Không có gì chờ xác nhận","Yêu cầu đặt lịch và đăng ký lớp đang chờ gia sư duyệt sẽ hiện ở đây.",null,null);
        if(tab==2)return classroom.emptyState(R.drawable.ic_file,"Chưa có buổi đã học","Các buổi học đã kết thúc sẽ hiện ở đây.",null,null);
        return classroom.emptyState(R.drawable.ic_calendar,"Chưa có buổi học sắp tới","Buổi học đã xác nhận và lớp nhóm bạn tham gia sẽ hiện ở đây.",null,null);
    }

    /** A white frame like Gia sư đang nhận lớp: the pages, then arrows and dots to move between them. */
    private LinearLayout pagerFrame(){
        LinearLayout frame=ui.column();frame.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        ui.cardSurface(frame,Ui.WHITE,ThemeColors.dark(requireContext())?Ui.BORDER:0);
        frame.setElevation(ui.dp(3));frame.setOutlineAmbientShadowColor(0x226583AD);frame.setOutlineSpotShadowColor(0x446583AD);
        pager=new ViewPager2(requireContext());pager.setAdapter(pages);pager.setOffscreenPageLimit(1);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
            @Override public void onPageSelected(int position){if(pager!=null){renderBar();fitPager();}}
        });
        frame.addView(pager,ui.lp(-1,-2));
        bar=ui.row();bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(ui.dp(2),ui.dp(10),ui.dp(2),0);
        previous=arrow(true);following=arrow(false);dots=ui.row();dots.setGravity(Gravity.CENTER);
        bar.addView(previous,ui.lp(36,36));ui.weight(bar,dots);bar.addView(following,ui.lp(36,36));
        frame.addView(bar,ui.lp(-1,-2));
        return frame;
    }
    private View arrow(boolean back){
        FrameLayout button=new FrameLayout(requireContext());ui.surface(button,Ui.PALE,18,0);
        ImageView icon=ui.icon(R.drawable.ic_right,16,Ui.BLUE);if(back)icon.setRotation(180);
        button.addView(icon,new FrameLayout.LayoutParams(ui.dp(16),ui.dp(16),Gravity.CENTER));
        button.setContentDescription(back?"Trang trước":"Trang sau");
        ui.clickable(button,()->pager.setCurrentItem(pager.getCurrentItem()+(back?-1:1)));
        return button;
    }
    private void renderBar(){
        int count=pages.getItemCount(),current=pager.getCurrentItem();
        bar.setVisibility(count>1?View.VISIBLE:View.GONE);
        previous.setEnabled(current>0);previous.setAlpha(current>0?1f:.35f);
        following.setEnabled(current<count-1);following.setAlpha(current<count-1?1f:.35f);
        dots.removeAllViews();bar.setContentDescription("Trang "+(current+1)+" trên "+count);
        if(count>7){dots.addView(ui.text((current+1)+" / "+count,14,Ui.MUTED,true));return;}
        for(int i=0;i<count;i++){
            View dot=new View(requireContext());ui.surface(dot,i==current?Ui.BLUE:0xFFC9D8EE,3,0);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ui.dp(i==current?18:6),ui.dp(6));
            params.leftMargin=params.rightMargin=ui.dp(3);
            int page=i;ui.clickable(dot,()->pager.setCurrentItem(page));dots.addView(dot,params);
        }
    }
    /** The pager is as tall as the page on screen, so a short last page leaves no gap. */
    private void fitPager(){
        pager.post(()->{
            if(pager==null||pages.pages.isEmpty())return;
            int width=pager.getWidth()>0?pager.getWidth():list.getWidth()-frame.getPaddingLeft()-frame.getPaddingRight();
            if(width<=0)return;
            List<View> shown=pages.pages.get(Math.min(pager.getCurrentItem(),pages.pages.size()-1));
            int height=0;
            for(int i=0;i<shown.size();i++){
                shown.get(i).measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                height+=shown.get(i).getMeasuredHeight()+(i<shown.size()-1?ui.dp(11):0);
            }
            ViewGroup.LayoutParams params=pager.getLayoutParams();
            if(params.height!=height){params.height=height;pager.setLayoutParams(params);}
        });
    }
    private final class Pages extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        final List<List<View>> pages=new ArrayList<>();
        @Override public int getItemCount(){return pages.size();}
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){
            LinearLayout page=new LinearLayout(parent.getContext());page.setOrientation(LinearLayout.VERTICAL);
            page.setLayoutParams(new RecyclerView.LayoutParams(-1,-1));
            return new RecyclerView.ViewHolder(page){};
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,int position){
            LinearLayout page=(LinearLayout)holder.itemView;page.removeAllViews();
            List<View> shown=pages.get(position);
            for(int i=0;i<shown.size();i++){
                View card=shown.get(i);
                if(card.getParent()!=null)((ViewGroup)card.getParent()).removeView(card);
                LinearLayout.LayoutParams params=ui.lp(-1,-2);if(i<shown.size()-1)params.bottomMargin=ui.dp(11);page.addView(card,params);
            }
        }
    }

    /** One coming session of a class; the soonest one also lists the sessions after it. */
    private Entry upcoming(GroupClass item,GroupClass.Session session,List<GroupClass.Session> ahead,Object snapshot) {
        boolean next=session==ahead.get(0);int later=next?ahead.size()-1:0;
        boolean live=item.online()&&session.roomOpen();
        return new Entry("session/"+item.id+"/"+session.number,session.start(),
                Arrays.asList(snapshot,session.start(),live,item.proposalOpen(),later),()->{
                    View move=moveNote(item,session);
                    LinearLayout extra=null;
                    if(move!=null||later>0){
                        extra=ui.column();
                        if(move!=null)ui.add(extra,move);
                        if(move!=null&&later>0)ui.space(extra,8);
                        if(later>0)extra.addView(ui.link("Xem "+later+" buổi tiếp theo ›",14,()->laterSessions(item,ahead)),ui.lp(-2,-2));
                    }
                    return classCard(item,session.label(),ui.pill("Buổi "+session.number+"/"+item.sessions,14,Ui.GREEN,Ui.GREEN_BG),
                            "Buổi "+session.number+" trên tổng số "+item.sessions+" buổi.",live,extra);
                });
    }
    /** The sessions after the next one: a header for the class, then one framed line per session. */
    private void laterSessions(GroupClass item,List<GroupClass.Session> ahead) {
        LinearLayout view=ui.column();view.setPadding(ui.dp(18),ui.dp(18),ui.dp(18),ui.dp(14));
        LinearLayout head=ui.row();head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(ui.dp(12),ui.dp(12),ui.dp(12),ui.dp(12));
        ui.cardSurface(head,Ui.PALE,0);
        head.addView(ui.photo(item.art(),null,58,58,14));ui.gap(head,12);
        LinearLayout heading=ui.column();
        TextView title=ui.text(item.title,18,Ui.INK,true);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ui.add(heading,title);ui.space(heading,3);ui.add(heading,ui.text(item.tutorName+" · "+item.mode,13,Ui.MUTED,false));ui.space(heading,7);
        heading.addView(ui.pill((ahead.size()-1)+" buổi tiếp theo",13,Ui.WHITE,Ui.BLUE),ui.lp(-2,-2));
        ui.weight(head,heading);ui.add(view,head);ui.space(view,14);
        for(GroupClass.Session session:ahead.subList(1,ahead.size())) {
            LinearLayout row=ui.row();row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(ui.dp(10),ui.dp(9),ui.dp(12),ui.dp(9));
            ui.fieldSurface(row,Ui.WHITE,Ui.BORDER);
            TextView number=ui.text(String.valueOf(session.number),15,Ui.BLUE,true);number.setGravity(Gravity.CENTER);ui.surface(number,Ui.PALE,19,0);
            row.addView(number,ui.lp(38,38));ui.gap(row,12);
            LinearLayout text=ui.column();
            ui.add(text,ui.text(GroupClass.DAYS[session.date.getDayOfWeek().getValue()-1]+", "+session.date.format(Lesson.DATE),15,Ui.INK,true));ui.space(text,2);
            ui.add(text,ui.text(session.timeLabel()+" · Buổi "+session.number+"/"+item.sessions,13,Ui.MUTED,false));
            ui.weight(row,text);
            if(session.moved()){ui.gap(row,6);row.addView(ui.pill("Đã đổi lịch",12,Ui.GREEN,Ui.GREEN_BG),ui.lp(-2,-2));}
            ui.add(view,row);ui.space(view,8);
        }
        ui.space(view,6);
        AlertDialog dialog=ui.dialog().create();
        ui.addAction(view,ui.action("Đóng",0,Ui.PRIMARY,dialog::dismiss),46);
        ScrollView scroll=new ScrollView(classroom.context());scroll.addView(view);
        dialog.setView(scroll);dialog.show();
    }

    /** Shortcut to Lịch sử đăng ký lớp học with the number of registrations still waiting. */
    private View registrationsLink() {
        int sent = 0, waiting = 0;
        for (GroupClass item : classroom.store.classes)
            for (GroupClass.Registration registration : item.registrations)
                if (registration.studentId.equals(classroom.me())) { sent++; if (registration.pending()) waiting++; }
        LinearLayout row = ui.row(); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(ui.dp(14),ui.dp(11),ui.dp(10),ui.dp(11));
        ui.fieldSurface(row,Ui.WHITE,Ui.BORDER);
        row.addView(ui.icon(R.drawable.ic_file,22,Ui.BLUE)); ui.gap(row,12);
        LinearLayout text = ui.column(); ui.add(text,ui.text("Lớp đã đăng ký",16,Ui.INK,true)); ui.space(text,2);
        ui.add(text,ui.text(sent==0?"Xem trạng thái các lớp bạn đăng ký":sent+" đăng ký"+(waiting>0?" · "+waiting+" chờ duyệt":""),13,Ui.MUTED,false));
        ui.weight(row,text);
        if (waiting > 0) { row.addView(ui.pill(String.valueOf(waiting),13,Ui.WHITE,Ui.ORANGE),ui.lp(-2,-2)); ui.gap(row,6); }
        row.addView(ui.icon(R.drawable.ic_right,20,Ui.BLUE));
        row.setContentDescription("Lớp đã đăng ký"); ui.clickable(row,classroom::openRegistrations); return row;
    }

    private View lessonCard(Lesson lesson) {
        LinearLayout card=ui.bordered();card.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.avatar(lesson.tutorId,lesson.tutorName,73));ui.gap(row,14);
        LinearLayout details=ui.column();ui.space(details,4);
        LinearLayout titleRow=ui.row();titleRow.setGravity(Gravity.TOP);
        TextView title=ui.text(lesson.title,lesson.title.length()>14?14:17,Ui.INK,true);ui.weight(titleRow,title);ui.gap(titleRow,4);
        titleRow.addView(ui.lessonStatus(lesson));ui.add(details,titleRow);ui.space(details,2);
        TextView teacher=ui.text(lesson.tutorName,15,Ui.MUTED,false);ui.clickable(teacher,()->classroom.openTutor(lesson.tutorId,lesson.tutorName));ui.add(details,teacher);ui.space(details,8);
        ui.add(details,ui.fact(R.drawable.ic_calendar,lesson.dateLabel()+" · "+lesson.timeLabel(),18,14,Ui.MUTED,false));
        ui.add(details,ui.fact(lesson.mode.equals("Tại nhà")?R.drawable.ic_home_soft:R.drawable.ic_video,lesson.mode+(lesson.trial?" · Học thử":""),18,15,Ui.MUTED,false));
        ui.weight(row,details);ui.add(card,row);
        if(lesson.proposal()&&lesson.confirmed()) {
            ui.space(card,8);LinearLayout proposal=ui.note(R.drawable.ic_clock,"Gia sư đề nghị đổi lịch. Chạm để xem.",Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE);
            ui.clickable(proposal,()->{classroom.lessonId=lesson.id;classroom.show("reschedule");});ui.add(card,proposal);
        }
        ui.space(card,9);
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",0,Ui.OUTLINE,()->classroom.message(lesson.tutorName)),38);ui.gap(actions,9);
        ui.weightAction(actions,ui.action(lesson.needsConfirmation()?"Xác nhận":"Chi tiết",0,Ui.PRIMARY,()->classroom.openLesson(lesson)),38);
        ui.add(card,actions);return card;
    }

    /** A tutor account also books lessons as a student; this switches to its teaching side. */
    private void tutorMode() {
        classroom.ui.dialog().setTitle("Chuyển sang chế độ gia sư?")
                .setMessage("Bạn sẽ chuyển sang giao diện gia sư để duyệt yêu cầu, xem lịch dạy và quản lý lớp nhóm.")
                .setNegativeButton("Hủy",null).setPositiveButton("Chuyển",(d,w)->classroom.setTutorMode(true)).show();
    }

    /** A group class session or registration, laid out like a lesson card; "Chi tiết" summarises the class. */
    private View classCard(GroupClass item, String when, TextView status, String note) { return classCard(item,when,status,note,false,null); }
    private View classCard(GroupClass item, String when, TextView status, String note, boolean live, View extra) {
        LinearLayout card=ui.bordered();card.setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));
        LinearLayout row=ui.row();row.setGravity(Gravity.TOP);
        row.addView(ui.photo(item.art(),null,73,77,12));ui.gap(row,14);
        LinearLayout details=ui.column();ui.space(details,4);
        LinearLayout titleRow=ui.row();titleRow.setGravity(Gravity.TOP);
        TextView title=ui.text(item.title,item.title.length()>14?14:17,Ui.INK,true);ui.weight(titleRow,title);ui.gap(titleRow,4);
        titleRow.addView(status);ui.add(details,titleRow);ui.space(details,2);
        TextView teacher=ui.text(item.tutorName,15,Ui.MUTED,false);ui.clickable(teacher,()->classroom.openTutor(item.tutorId,item.tutorName));ui.add(details,teacher);ui.space(details,8);
        ui.add(details,ui.fact(R.drawable.ic_calendar,when,18,14,Ui.MUTED,false));
        ui.add(details,ui.fact(R.drawable.ic_users,item.mode+" · Lớp nhóm",18,15,Ui.MUTED,false));
        ui.weight(row,details);ui.add(card,row);
        if(extra!=null){ui.space(card,8);ui.add(card,extra);}
        ui.space(card,9);
        String summary=note+"\n\n"+item.subject+" · "+item.level+"\nLịch học: "+item.scheduleLabel()+"\nKhai giảng: "+item.startLabel()+" · "+item.sessions+" buổi\n"
                +item.mode+(item.address.isEmpty()?"":" · "+item.address)+(item.online()?"\nPhòng học: "+(item.roomLink.isEmpty()?"phòng Jitsi của app":item.roomHost()):"")+"\nSĩ số: "+item.members.size()+"/"+item.capacity+"\nHọc phí: "+item.priceLabel();
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Nhắn tin",0,Ui.OUTLINE,()->classroom.message(item.tutorName)),38);ui.gap(actions,9);
        ui.weightAction(actions,live?ui.action("Vào phòng học",R.drawable.ic_video,Ui.PRIMARY,()->classroom.openClassRoom(item))
                :ui.action("Chi tiết",0,Ui.PRIMARY,item.cancelled()?()->classroom.openRegistration(item):()->classroom.dialog(item.title,summary)),38);
        ui.add(card,actions);return card;
    }

    /** The tutor's proposal for this session and the student's answer, or where a moved session came from. */
    private View moveNote(GroupClass item,GroupClass.Session session) {
        if(item.proposalOpen()&&item.proposal.number==session.number) {
            String answer=item.proposal.answers.get(classroom.me()),target=item.proposal.move.label(item.minutes);
            LinearLayout note=GroupClass.Proposal.JOIN.equals(answer)
                    ?ui.note(R.drawable.ic_calendar,"Bạn tham gia được giờ mới "+target+".\nChạm để xem hoặc đổi câu trả lời.",Ui.PALE,Ui.BLUE,Ui.INK)
                    :GroupClass.Proposal.BUSY.equals(answer)
                    ?ui.note(R.drawable.ic_info,"Bạn đã báo bận giờ mới "+target+".\nBuổi học vẫn giữ giờ cũ. Chạm để đổi câu trả lời.",0xFFFFE9EC,Ui.RED,Ui.INK)
                    :ui.note(R.drawable.ic_clock,"Gia sư đề nghị đổi buổi này sang "+target+".\nChạm để trả lời.",Ui.ORANGE_BG,Ui.BLUE,Ui.ORANGE);
            ui.clickable(note,()->answerMove(item));return note;
        }
        if(!session.moved())return null;
        return ui.note(R.drawable.ic_calendar,"Đã đổi lịch từ "+session.plannedLabel()+".\nLý do: "+session.move.reason,Ui.GREEN_BG,Ui.GREEN,Ui.INK);
    }

    /** Both slots, the reason and the deadline; the student answers whether they can come at the new time. */
    private void answerMove(GroupClass item) {
        if(!item.proposalOpen()){classroom.show("schedule");return;}
        GroupClass.Proposal proposal=item.proposal;GroupClass.Session session=item.sessionList().get(proposal.number-1);
        String answer=proposal.answers.get(classroom.me()),clash=classroom.store.moveConflict(item);
        LinearLayout view=ui.column();view.setPadding(ui.dp(20),ui.dp(20),ui.dp(20),ui.dp(18));
        LinearLayout top=ui.row();top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(ui.photo(item.art(),null,52,52,12));ui.gap(top,14);
        LinearLayout heading=ui.column();
        TextView title=ui.text(item.title,19,Ui.INK,true);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ui.add(heading,title);ui.space(heading,4);ui.add(heading,ui.text("Buổi "+proposal.number+"/"+item.sessions+" · "+item.tutorName,14,Ui.MUTED,false));
        ui.weight(top,heading);ui.add(view,top);ui.space(view,16);
        ui.add(view,RescheduleFragment.slot(ui,"Lịch hiện tại",session.label(),Ui.PALE,Ui.INK));ui.space(view,10);
        ui.add(view,RescheduleFragment.slot(ui,"Lịch đề xuất",proposal.move.label(item.minutes),Ui.GREEN_BG,Ui.GREEN));ui.space(view,14);
        ui.label(view,"Lý do đổi lịch");
        TextView reason=ui.text(proposal.move.reason,15,Ui.INK,false);reason.setPadding(ui.dp(12),ui.dp(10),ui.dp(12),ui.dp(10));
        ui.fieldSurface(reason,Ui.PALE,Ui.BORDER);ui.add(view,reason);ui.space(view,12);
        if(clash!=null){ui.add(view,ui.note(R.drawable.ic_info,"Giờ đề xuất trùng với "+clash+" của bạn.",0xFFFFE9EC,Ui.RED,Ui.INK));ui.space(view,10);}
        ui.add(view,ui.note(R.drawable.ic_clock,"Trả lời trước "+proposal.deadlineLabel()+".\nKhông ai báo bận thì buổi học chuyển sang giờ mới.",Ui.PALE,Ui.BLUE,Ui.MUTED));
        if(answer!=null){
            ui.space(view,10);
            ui.add(view,ui.fact(GroupClass.Proposal.JOIN.equals(answer)?R.drawable.ic_calendar:R.drawable.ic_info,
                    "Bạn đã trả lời: "+(GroupClass.Proposal.JOIN.equals(answer)?"Tham gia được":"Bận"),18,14,Ui.MUTED,false));
        }
        ui.space(view,16);
        AlertDialog dialog=ui.dialog().create();
        LinearLayout actions=ui.row();
        ui.weightAction(actions,ui.action("Báo bận",0,Ui.OUTLINE,()->{dialog.dismiss();answer(item,false);}),46);ui.gap(actions,10);
        ui.weightAction(actions,ui.action("Tham gia được",0,Ui.PRIMARY,()->{dialog.dismiss();answer(item,true);}),46);
        ui.add(view,actions);
        TextView close=ui.link("Đóng",15,dialog::dismiss);close.setTextColor(Ui.MUTED);close.setGravity(Gravity.CENTER);
        ui.space(view,6);ui.add(view,close);
        android.widget.ScrollView scroll=new android.widget.ScrollView(classroom.context());scroll.addView(view);
        dialog.setView(scroll);dialog.show();
    }
    private void answer(GroupClass item,boolean join) {
        String error=classroom.store.answerMove(item,join);
        if(error!=null){classroom.dialog("Chưa gửi được câu trả lời",error);return;}
        classroom.show("schedule");
        classroom.notice(join?"Đã báo gia sư: bạn tham gia được giờ mới.":"Đã báo bận. Gia sư sẽ giữ giờ cũ hoặc đề nghị giờ khác.");
    }

    /** A card with the time it is sorted by, so lessons and class sessions share one list. */
    private static final class Entry {
        final String id;final LocalDateTime start;final Object content;final Supplier<View> create;
        Entry(String id,LocalDateTime start,Object content,Supplier<View> create){this.id=id;this.start=start;this.content=content;this.create=create;}
    }

    @Override public void onDestroyView(){
        if(body!=null)android.transition.TransitionManager.endTransitions(body);
        cards.clear();pages.pages.clear();body=null;list=null;tabs=null;scroll=null;history=null;tutorSwitch=null;renderedTab=-1;
        frame=null;bar=null;dots=null;pager=null;previous=null;following=null;
        super.onDestroyView();
    }

}
