package com.example.tmdt.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.RenderSnapshot;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AdminContractsFragment extends AdminScreen {
    private LinearLayout body,tabs;
    private int statusFilter;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);if(saved!=null)statusFilter=saved.getInt("contract_status_filter",0);}
    @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("contract_status_filter",statusFilter);}
    @Override protected View build(){LinearLayout root=root("Hợp đồng");tabs=ui.column();ui.add(root,tabs);body=ui.page(root);render();return root;}
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(state(),statusFilter);}
    private void render(){
        StableViews navigation=StableViews.at(tabs);navigation.begin();
        navigation.add("tabs",state().contractTab,()->{
            LinearLayout box=ui.column();ui.tabs(box,new String[]{"Giảng dạy","Với hệ thống"},state().contractTab,index->{
                if(index==state().contractTab)return;state().contractTab=index;state().tutorQuery="";scrollToTop();
                if(index==1&&!state().selectedTutor.isEmpty())state().selectAgreement(state().selectedTutor);
                refreshData();
            });return box;
        },ui.dp(12));navigation.commit();
        StableViews rows=StableViews.at(body);rows.begin();if(!feedback(rows)){rows.commit();return;}
        rows.add("templates",true,()->{
            LinearLayout box=ui.column();ui.addAction(box,ui.action("Quản lý mẫu hợp đồng",R.drawable.ic_file,Ui.OUTLINE,()->host().exchange.openTemplates()),46);return box;
        },ui.dp(14));
        rows.add("teacher-filter",Arrays.asList(state().selectedTutor,state().tutorDirectory()),()->
            ui.picker(R.drawable.ic_users,ui.value(state().selectedTutor.isEmpty()?"Tất cả gia sư":name(state().selectedTutor,"Gia sư")),this::chooseTutor),ui.dp(12));
        rows.add("search",state().contractTab,()->{LinearLayout box=ui.column();search(box,"Tìm hợp đồng, gia sư hoặc học viên",state().tutorQuery,value->{state().tutorQuery=value;refreshData();});return box;},0);
        if(state().contractTab==0)teaching(rows);else platform(rows);
        if(state().moreTutors)rows.add("more-tutors",Arrays.asList(state().loadingTutors,state().loadingTutorProfiles),()->{
            LinearLayout box=ui.column();action(box,"Tải thêm gia sư vào bộ lọc",Ui.OUTLINE,()->state().moreTutors());return box;
        },0);
        rows.commit();
    }
    private void teaching(StableViews rows){
        rows.add("status-filter",statusFilter,()->{
            LinearLayout box=ui.column();ui.tabs(box,new String[]{"Tất cả","Đã ký","Chờ ký"},statusFilter,index->{statusFilter=index;refreshData();});return box;
        },ui.dp(14));
        boolean selected=!state().selectedTutor.isEmpty(),loading=selected?state().loadingTutorContracts:state().loadingContracts;
        List<PaymentState.Row> contracts=new ArrayList<>((selected?state().tutorContracts:state().contracts).values());
        contracts.sort(Comparator.comparingLong((PaymentState.Row row)->row.time("created_at")).reversed());
        String query=CatalogState.normalize(state().tutorQuery);int count=0;
        for(PaymentState.Row contract:contracts){
            if(statusFilter==1&&!state().signedContract(contract))continue;
            if(statusFilter==2&&!Contract.PENDING.equals(contract.text("status")))continue;
            if(!CatalogState.normalize(contract.text("code")+" "+contract.text("subject")+" "+contract.text("tutor_name")+" "+contract.text("student_name")).contains(query))continue;
            count++;rows.add("contract/"+contract.id,contract,()->{LinearLayout box=ui.column();contractCard(box,contract);return box;},0);
        }
        if(count==0&&state().error.isEmpty())rows.add("empty-contracts",Arrays.asList(loading,query,statusFilter),()->ui.text(loading?"Đang tải hợp đồng…":"Không có hợp đồng phù hợp với bộ lọc.",14,Ui.MUTED,false),ui.dp(14));
        if(selected?state().moreTutorContracts:state().moreContracts)rows.add("more-contracts",Arrays.asList(selected,loading),()->{
            LinearLayout box=ui.column();action(box,loading?"Đang tải…":"Tải thêm hợp đồng",loading?Ui.DISABLED:Ui.OUTLINE,()->{if(selected)state().moreTutorContracts();else state().moreContracts();});return box;
        },0);
    }
    private void platform(StableViews rows){
        String uid=state().selectedTutor,query=CatalogState.normalize(state().tutorQuery);
        List<PaymentState.Row> agreements=new ArrayList<>();
        if(uid.isEmpty())agreements.addAll(state().agreements.values());else{PaymentState.Row agreement=state().agreement(uid);if(agreement!=null)agreements.add(agreement);}
        agreements.sort(Comparator.comparingLong((PaymentState.Row row)->row.time("signed_at")).reversed());int count=0;
        for(PaymentState.Row agreement:agreements){
            String tutorName=name(agreement.id,agreement.text("ho_ten"));
            if(!CatalogState.normalize(tutorName+" "+agreement.text("tieu_de")).contains(query))continue;
            count++;rows.add("agreement/"+agreement.id,Arrays.asList(agreement,tutorName),()->{
                LinearLayout card=ui.card(),top=ui.row(),copy=ui.column();top.addView(ui.avatar(agreement.id,tutorName,44));ui.gap(top,12);
                ui.add(copy,ui.text(tutorName,16,Ui.INK,true));ui.space(copy,5);
                ui.add(copy,ui.text(agreement.text("tieu_de").isEmpty()?"Hợp đồng cộng tác":agreement.text("tieu_de"),13,Ui.MUTED,false));ui.weight(top,copy);
                top.addView(ui.icon(R.drawable.ic_right,18,Ui.BLUE));ui.add(card,top);ui.space(card,12);
                LinearLayout status=ui.row();status.addView(ui.pill("DA_KY".equals(agreement.text("trang_thai"))?"Đã ký":agreement.text("trang_thai"),11,Ui.GREEN,Ui.GREEN_BG));
                ui.gap(status,10);ui.weight(status,ui.text("Phiên bản "+agreement.number("phien_ban"),12,Ui.MUTED,false));ui.add(card,status);
                ui.clickable(card,()->agreement(agreement.id));return card;
            },ui.dp(12));
        }
        boolean loading=uid.isEmpty()?state().loadingAgreements:state().loadingAgreement;
        if(count==0&&state().error.isEmpty())rows.add("empty-agreements",Arrays.asList(uid,loading,query),()->ui.text(loading?"Đang tải hợp đồng với hệ thống…":uid.isEmpty()?"Chưa có hợp đồng cộng tác phù hợp.":"Gia sư chưa có hợp đồng cộng tác phù hợp.",14,Ui.MUTED,false),ui.dp(14));
        if(uid.isEmpty()&&state().moreAgreements)rows.add("more-agreements",loading,()->{
            LinearLayout box=ui.column();action(box,"Tải thêm hợp đồng cộng tác",Ui.OUTLINE,()->state().moreAgreements());return box;
        },0);
    }
    private void chooseTutor(){
        List<PaymentState.Row> tutors=new ArrayList<>(state().tutorDirectory().values());
        tutors.sort(Comparator.comparing(row->row.text("ho_ten"),String.CASE_INSENSITIVE_ORDER));
        String[] labels=new String[tutors.size()+1];labels[0]="Tất cả gia sư";
        for(int i=0;i<tutors.size();i++)labels[i+1]=tutors.get(i).text("ho_ten").isEmpty()?"Gia sư "+tutors.get(i).id:tutors.get(i).text("ho_ten");
        host().hideKeyboard();ui.dialog().setTitle("Chọn gia sư").setItems(labels,(dialog,index)->{
            String uid=index==0?"":tutors.get(index-1).id;state().tutorQuery="";clearSearch();state().selectTutor(uid);scrollToTop();
            if(!uid.isEmpty())state().selectAgreement(uid);refreshData();
        }).setNegativeButton("Đóng",null).show();
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;tabs=null;}
}
