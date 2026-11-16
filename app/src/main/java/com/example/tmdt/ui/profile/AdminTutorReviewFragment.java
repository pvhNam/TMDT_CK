package com.example.tmdt.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.RenderSnapshot;
import java.util.*;

public final class AdminTutorReviewFragment extends TutorReviewScreen {
    private String filter="PENDING";
    private boolean detail;
    private LinearLayout body;
    @Override public void onCreate(Bundle state){super.onCreate(state);if(state!=null){detail=state.getBoolean("detail");filter=state.getString("filter","PENDING");}}
    @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("detail",detail);out.putString("filter",filter);}
    @Override protected View build(){
        LinearLayout root=ui.column();root.setBackgroundColor(0xFFF7F9FD);
        ui.header(root,"Duyệt gia sư",()->{if(!backToList())go("admin_management");},null);
        body=ui.page(root);render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(review.admin,review.busy,review.error,review.notice,detail,filter,review.selectedId,detail?review.selected:review.queue);}
    public boolean backToList(){if(!detail)return false;detail=false;scrollToTop();refreshData();return true;}
    private void render(){
        StableViews rows=StableViews.at(body);rows.begin();
        if(review.busy||!review.error.isEmpty()||!review.notice.isEmpty())rows.add("feedback",Arrays.asList(review.busy,review.error,review.notice),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        if(!review.admin){rows.add("permission",true,()->ui.text("Trang này dành cho quản trị viên.",16,Ui.INK,true),0);rows.commit();return;}
        if(detail){details(rows);rows.commit();return;}
        rows.add("filters",filter,()->{
            LinearLayout box=ui.column();String[] keys={"PENDING","NEEDS_REVISION","VERIFIED","REJECTED"},labels={"Chờ duyệt","Bổ sung","Đã duyệt","Từ chối"};
            int index=Arrays.asList(keys).indexOf(filter);ui.tabs(box,labels,Math.max(0,index),i->{filter=keys[i];refreshData();});return box;
        },ui.dp(16));
        int count=0;
        for(Map<String,Object> item:review.queue){String status=TutorReviewState.text(item,"trang_thai_xac_minh");
            if(!filter.equals(status)&&!(filter.equals("PENDING")&&status.equals("IN_REVIEW")))continue;count++;
            String uid=TutorReviewState.text(item,"id");rows.add("application/"+uid,item,()->{
            LinearLayout card=ui.card(),row=ui.row();row.addView(ui.avatar(uid,TutorReviewState.text(item,"ho_ten"),44));ui.gap(row,12);
            LinearLayout copy=ui.column();ui.add(copy,ui.text(TutorReviewState.text(item,"ho_ten"),17,Ui.INK,true));ui.space(copy,5);ui.add(copy,ui.text(label(status),13,Ui.MUTED,false));ui.weight(row,copy);row.addView(ui.text("›",26,Ui.BLUE,false));ui.add(card,row);
            card.setTag(uid);ui.clickable(card,()->{detail=true;scrollToTop();review.select(uid);refreshData();});return card;
            },ui.dp(12));
        }
        final int found=count;rows.add("count",count,()->ui.text(found+" hồ sơ",12,Ui.MUTED,false),0);rows.commit();
    }
    private void details(StableViews rows){
        Map<String,Object> item=review.selected;String uid=review.selectedId;
        rows.add("identity",Arrays.asList(uid,item.get("ho_ten"),item.get("mon_hoc"),item.get("cap_lop")),()->{LinearLayout box=ui.column();summary(box,item,uid);return box;},0);
        rows.add("information",Arrays.asList(item.get("truong"),item.get("chuyen_nganh"),item.get("so_nam_kinh_nghiem"),item.get("hinh_thuc_day"),item.get("hoc_phi")),()->{
        LinearLayout body=ui.card();ui.label(body,"Trường / đơn vị");ui.add(body,ui.text(TutorReviewState.text(item,"truong"),15,Ui.INK,false));ui.space(body,14);
        ui.label(body,"Chuyên ngành");ui.add(body,ui.text(TutorReviewState.text(item,"chuyen_nganh"),15,Ui.INK,false));ui.space(body,18);
        ui.label(body,"Thông tin giảng dạy");
        ui.add(body,ui.text("Kinh nghiệm: "+item.getOrDefault("so_nam_kinh_nghiem",0)+" năm",14,Ui.MUTED,false));
        String mode=TutorReviewState.text(item,"hinh_thuc_day");ui.add(body,ui.text("Hình thức: "+(mode.equals("CA_HAI")?"Trực tuyến · Tại nhà":mode.equals("TAI_NHA")?"Tại nhà":"Trực tuyến"),14,Ui.MUTED,false));
        Object price=item.get("hoc_phi");if(price instanceof Number)ui.add(body,ui.text("Học phí: "+Tutor.money(((Number)price).intValue())+" / 90 phút",14,Ui.BLUE,true));return body;
        },ui.dp(16));
        rows.add("evidence-title",true,()->ui.text("Minh chứng",16,Ui.INK,true),ui.dp(12));
        List<Map<String,Object>> evidence=TutorReviewState.files(item);
        for(int i=0;i<evidence.size();i++){Map<String,Object> file=evidence.get(i);rows.add("evidence/"+i,file,()->{LinearLayout box=ui.column();files(box,Collections.singletonList(file));return box;},0);}
        if(evidence.isEmpty())rows.add("empty-evidence",true,()->ui.text("Chưa có tệp minh chứng.",13,Ui.MUTED,false),ui.dp(12));
        rows.add("submitted",Arrays.asList(item.get("submitted_at"),item.get("created_at")),()->ui.text("Gửi ngày "+date(item,item.containsKey("submitted_at")?"submitted_at":"created_at"),12,Ui.MUTED,false),ui.dp(18));
        String status=TutorReviewState.text(item,"trang_thai_xac_minh");
        if(!Arrays.asList("PENDING","IN_REVIEW").contains(status)){rows.add("result",Arrays.asList(status,item.get("ly_do")),()->ui.text(label(status)+"\n"+TutorReviewState.text(item,"ly_do"),15,Ui.INK,true),0);return;}
        boolean hasPhoto=TutorReviewState.files(item).stream().anyMatch(f->"diploma".equals(f.get("kind"))&&TutorEvidenceDraft.isFirestoreImage(f));
        rows.add("actions",Arrays.asList(uid,status,hasPhoto,review.busy),()->{
        LinearLayout body=ui.column();
        if(status.equals("PENDING"))action(body,"Bắt đầu kiểm tra",Ui.OUTLINE,()->review.decide(uid,"IN_REVIEW",""));
        if(hasPhoto)action(body,"Chấp nhận làm giáo viên",Ui.PRIMARY,()->ui.dialog().setTitle("Chấp nhận làm giáo viên?").setMessage("Xác nhận ảnh minh chứng hợp lệ. Người dùng sẽ được mở chức năng giáo viên và nhận lớp.").setNegativeButton("Hủy",null).setPositiveButton("Chấp nhận",(d,w)->review.decide(uid,"VERIFIED","")).show());
        else{ui.add(body,ui.note(R.drawable.ic_info,"Chưa có ảnh bằng cấp/thẻ sinh viên. Yêu cầu người dùng bổ sung ảnh trước khi chấp nhận.",Ui.ORANGE_BG,Ui.ORANGE,Ui.INK));ui.space(body,12);}
        action(body,"Yêu cầu bổ sung",Ui.OUTLINE,()->reason("NEEDS_REVISION","Yêu cầu bổ sung"));
        action(body,"Từ chối hồ sơ",Ui.DANGER,()->reason("REJECTED","Lý do từ chối"));
        return body;
        },0);
    }
    private void reason(String status,String title){
        EditText input=ui.entry("Nhập lý do (tối đa 500 ký tự)",true);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        final String uid=review.selectedId;androidx.appcompat.app.AlertDialog dialog=ui.dialog().setTitle(title).setView(input).setNegativeButton("Hủy",null).setPositiveButton("Xác nhận",null).show();
        dialog.getButton(-1).setOnClickListener(v->{String value=input.getText().toString().trim();if(value.isEmpty()){input.setError("Nhập lý do trước khi xác nhận.");return;}review.decide(uid,status,value);dialog.dismiss();});
    }
    @Override public void onDestroyView(){super.onDestroyView();body=null;}
}
