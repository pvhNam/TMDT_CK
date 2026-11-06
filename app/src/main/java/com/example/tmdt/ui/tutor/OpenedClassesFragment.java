package com.example.tmdt.ui.tutor;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import com.example.tmdt.R;
import com.example.tmdt.Classroom;
import com.example.tmdt.ui.common.ScreenFragment;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.google.android.material.tabs.TabLayout;
import com.example.tmdt.GroupClass;
import com.example.tmdt.Tutor;

/** Screen 35 · Lớp đã mở (UC17): the tutor's classes by status, with review, edit and close actions. */
public final class OpenedClassesFragment extends ScreenFragment {
    private LinearLayout listContainer;
    private TabLayout tabs;
    private static final String[] STATUSES = {GroupClass.OPEN, GroupClass.RUNNING, GroupClass.ENDED};

    @Override protected View build() {
        LinearLayout root = ui.column();
        View open = ui.action("＋ Mở lớp",0,Ui.PRIMARY,()->classroom.openClass(null,"openClass"));
        open.setLayoutParams(ui.lp(104,37));
        ui.title(root,"Lớp đã mở",open);
        tabs=new TabLayout(requireContext());tabs.setTabMode(TabLayout.MODE_FIXED);
        for(String title:new String[]{"Đang tuyển","Đang học","Đã kết thúc"})tabs.addTab(tabs.newTab().setText(title),false);
        tabs.getTabAt(classroom.classTab).select();ui.add(root,tabs);
        listContainer=ui.page(root);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override public void onTabSelected(TabLayout.Tab tab){
                if(tab.getPosition()==classroom.classTab)return;
                classroom.classTab=tab.getPosition();renderClasses();
                ((android.widget.ScrollView)listContainer.getParent()).scrollTo(0,0);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab){}
            @Override public void onTabReselected(TabLayout.Tab tab){}
        });
        renderClasses();return root;
    }
    @Override protected boolean updateContent(){if(listContainer==null)return false;renderClasses();return true;}
    private void renderClasses(){
        String[] labels = {"Đang tuyển","Đang học","Đã kết thúc"};
        for(int i=0;i<3;i++)tabs.getTabAt(i).setText(labels[i]+" ("+mine(STATUSES[i]).size()+")");
        if(tabs.getSelectedTabPosition()!=classroom.classTab)tabs.getTabAt(classroom.classTab).select();
        StableViews rows=StableViews.at(listContainer);rows.begin();
        int groups=GroupMatch.groups(classroom.store.openRequests,classroom.me()).size();
        if(classroom.classTab==0&&groups>0)rows.add("matches",groups,this::matchLink,ui.dp(12));
        for(int i=0;i<3;i++)labels[i]+=" ("+mine(STATUSES[i]).size()+")";
        ui.tabs(tabContainer,labels,classroom.classTab,index->{
            if(index==classroom.classTab)return;
            classroom.classTab=index;renderClasses();
            ((android.widget.ScrollView)listContainer.getParent()).scrollTo(0,0);
            com.example.tmdt.ui.common.Motion.reveal(listContainer);
        });
        ui.space(listContainer,4);
        List<GroupClass> list=mine(STATUSES[classroom.classTab]);
        for(GroupClass item:list)rows.add("class/"+item.id,item,()->card(item),ui.dp(11));
        rows.commit();
    }

    @Override public void onDestroyView(){super.onDestroyView();listContainer=null;tabs=null;}

    private List<GroupClass> mine(String status) {
        List<GroupClass> list = new ArrayList<>();
        for (GroupClass item : classroom.store.classes) if (item.tutorId.equals(classroom.me()) && (item.status.equals(status) || (GroupClass.ENDED.equals(status) && item.cancelled()))) list.add(item);
        return list;
    }

    private View card(GroupClass item) {
        LinearLayout card = ui.bordered(); card.setPadding(ui.dp(10),ui.dp(13),ui.dp(10),ui.dp(10));
        LinearLayout top = ui.row(); top.setGravity(Gravity.TOP);
        top.addView(ui.photo(item.art(),null,77,83,12)); ui.gap(top,11);
        LinearLayout info = ui.column(); LinearLayout titleRow = ui.row(); titleRow.setGravity(Gravity.TOP);
        ui.weight(titleRow,ui.text(item.title,item.title.length()>16?16:21,Ui.INK,true)); ui.gap(titleRow,6);
        titleRow.addView(status(item)); ui.add(info,titleRow); ui.space(info,4);
        ui.add(info,ui.fact(R.drawable.ic_users,item.members.size()+"/"+item.capacity+" học viên",18,16,Ui.INK,false));
        if (item.pendingCount() > 0) ui.add(info,ui.fact(R.drawable.ic_clock,item.pendingCount()+" đăng ký chờ duyệt",18,16,Ui.ORANGE,false));
        if (item.cancelled()) ui.add(info,ui.fact(R.drawable.ic_info,"Lý do: "+item.cancelReason,18,14,Ui.RED,false));
        ui.add(info,ui.fact(item.mode.equals("Tại nhà")?R.drawable.ic_home_soft:R.drawable.ic_video,item.sessions+" buổi · "+item.mode,18,16,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_coin,Tutor.money(item.price)+(item.perSession?" / buổi":" / học viên"),18,16,Ui.INK,false));
        ui.add(info,ui.fact(R.drawable.ic_calendar,item.scheduleLabel(),18,16,Ui.INK,false));
        ui.weight(top,info); ui.add(card,top); ui.space(card,8);

        if (GroupClass.OPEN.equals(item.status) && item.pendingCount() > 0) {
            ui.addAction(card,ui.action("Duyệt đăng ký",0,Ui.PRIMARY,()->classroom.openClass(item,"review")),40); ui.space(card,10);
            LinearLayout row = ui.row();
            ui.weightAction(row,ui.action("Thành viên",R.drawable.ic_users,Ui.OUTLINE,()->members(classroom,item)),40); ui.gap(row,10);
            ui.weightAction(row,ui.action("Chỉnh sửa",R.drawable.ic_edit,Ui.OUTLINE,()->classroom.openClass(item,"openClass")),40);
            ui.add(card,row); ui.space(card,9);
            ui.addAction(card,ui.action("⊘  Đóng tuyển sinh",0,Ui.DANGER,()->close(classroom,item)),38);
        } else {
            ui.addAction(card,ui.action("Xem lớp",0,Ui.OUTLINE,()->classroom.openClass(item,"review")),38);
        }
        return card;
    }

    private TextView status(GroupClass item) {
        if (GroupClass.OPEN.equals(item.status)) return ui.pill("Đang tuyển",12,Ui.ORANGE,Ui.ORANGE_BG);
        if (GroupClass.RUNNING.equals(item.status)) return ui.pill("Đang học",12,Ui.GREEN,Ui.GREEN_BG);
        if (item.cancelled()) return ui.pill("Đã hủy",12,Ui.RED,0xFFFFE9EC);
        return ui.pill("Đã kết thúc",12,Ui.MUTED,0xFFEEF2F7);
    }

    public static void members(Classroom classroom, GroupClass item) {
        StringBuilder list = new StringBuilder();
        for (String member : item.memberNames()) list.append("• ").append(member).append('\n');
        classroom.dialog("Thành viên ("+item.members.size()+"/"+item.capacity+")",list.length()==0?"Lớp chưa có học viên.":list.toString().trim());
    }

    public static void close(Classroom classroom, GroupClass item) {
        classroom.ui.dialog().setTitle("Đóng tuyển sinh?")
                .setMessage("Lớp \""+item.title+"\" sẽ chuyển sang mục Đang học với "+item.members.size()+" học viên. Các đăng ký đang chờ sẽ không được duyệt thêm.")
                .setNegativeButton("Hủy",null)
                .setPositiveButton("Đóng tuyển sinh",(dialog,which)->{
                    classroom.store.closeRecruiting(item); classroom.classTab=1; classroom.show("classes"); classroom.notice("Đã đóng tuyển sinh lớp \""+item.title+"\".");
                }).show();
    }
}
